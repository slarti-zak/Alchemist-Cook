# Migrate AlchemistCook from Couchbase Lite to WebDAV file storage

## Context

AlchemistCook currently stores all data (recipes, shopping lists, timers, cooking state) in a per-user Couchbase Lite database that continuously replicates to a Couchbase Sync Gateway server. This is heavyweight infrastructure for a personal recipe app: it requires running/paying for a Sync Gateway deployment, bundles Couchbase's own replication protocol and local SQLite engine, and stores recipe images as opaque database blobs rather than as files a user could browse or back up independently.

The goal is to replace this with a WebDAV-based store: recipes become markdown files (the `Recipe.content` field is already markdown) with YAML front matter for structured fields, images become plain files living alongside the recipe in a per-recipe folder, and the whole tree syncs to any WebDAV server (Nextcloud, etc.) the user points the app at, in the background.

Decisions confirmed with the user before finalizing this plan:
- **Conflicts**: keep-both. On a detected conflict, write the losing version as a sibling `*.conflict-<timestamp>.md`/image file rather than silently discarding it.
- **Existing data**: a one-time Couchbase → WebDAV migration/export tool is required (not a fresh start).
- **Sharing**: investigation found today's Couchbase `owner`/`"!"`-channel scheme is vestigial — every account only ever reads/writes its own documents, and there's no sharing UI. The user nonetheless wants **real multi-user sharing built** as part of this migration (not just parity), so the new design introduces an explicit multi-library concept (below) rather than reproducing the old scaffolding.

## Architecture Overview

- **File format**: one folder per recipe (`<slug>-<shortId>/`) containing `recipe.md` (YAML front matter + the existing markdown `content` body) and an image file (`image.jpg`) referenced by filename in the front matter. Non-prose entities (shopping lists, planned/active recipes, timers) are stored as small YAML files under a `.state/` subtree, since they're structured data, not prose meant for browsing.
- **Multi-library model (replaces owner/channel scheme)**: a "library" = one WebDAV connection (URL + credentials) + its own local mirror + its own slice of the local index. Every install has a default personal library; the user can add additional shared libraries (each pointing at a WebDAV folder that other users' installs are separately configured to point at — access control is delegated to whatever the WebDAV server provides, e.g. Nextcloud folder shares, since WebDAV itself has no ACL concept the app can manage). A recipe belongs to exactly one library at a time; a "Share…" action moves it from personal into a chosen shared library.
- **Local mirror + index**: each library gets an app-private directory mirroring its remote tree, plus a Room database that indexes the parsed contents of that tree (replacing Couchbase Lite's local SQLite query engine — flat files have no native query capability).
- **WebDAV client**: hand-rolled, OkHttp-based (PROPFIND/GET/PUT/DELETE/MKCOL + Basic Auth). No suitable maintained library exists; the protocol surface needed is small.
- **Sync engine**: PROPFIND-diff based — compare remote ETags/mtimes against last-known-synced state, pull changed/new remote files, push changed/new local files, keep-both on conflicting changes, then rebuild the affected Room index rows from the resulting files.
- **Background sync**: reuses the existing `BackgroundService`/`WorkManagerBackgroundService` periodic-WorkManager pattern (`service/background/`), replacing `SyncWork.kt`'s Couchbase replicator with a call into `SyncEngine`. Since WebDAV has no push/continuous-replication mechanism, "live" sync becomes: immediate push on local write, pull on app resume, and periodic WorkManager pull+push while backgrounded. This raises cross-device sync latency vs. today's continuous replicator — an accepted tradeoff.

## File/Folder Format

- Recipe front matter (YAML) carries: `id`, `serves`, `ingredients` (list of name/amount/unitCategory), `extendedContent` (the optional `RecipeGraph`), `updatedAt`. `name` can double as the H1/title in the markdown body or stay in front matter — keep it in front matter for simple parsing, matching today's `Recipe.name` field. Body = existing `content` string, unchanged.
- `id` stays a generated UUID (persisted in front matter) so `ShoppingListItem.recipeId` / `PlannedRecipe.recipeId` / `RunningTimer.recipeId` foreign keys survive folder renames. Folder name = `slugify(name)-<first 8 chars of id>` for human browsability.
- Image key today is a single Couchbase blob (`"image"`); store as a single file (`image.<ext>`) referenced by filename in front matter — no need to support multiple images, matches current capability.
- `DatabaseSettings`-style bookkeeping (today: `id = "database-settings"` singleton doc used to throttle Couchbase maintenance) has no WebDAV equivalent need (that was SQLite compaction); drop it. A new small per-library "last synced state" table lives only in the local Room DB, not synced.
- Reuse the existing Jackson setup (`jackson-module-kotlin` + the custom `BigDecimalSerializer`/`DbDurationSerializer`/etc. in `service/couchbase/json/`) via `jackson-dataformat-yaml`, relocated to a shared `service/serialization/` package usable by both the new store and (temporarily) the old Couchbase code during migration.

## New Components

1. `service/webdav/WebDavClient.kt` — OkHttp-based PROPFIND (parses multistatus XML for name/etag/mtime/collection-flag)/GET/PUT/DELETE/MKCOL, Basic Auth.
2. `service/store/LocalMirror.kt` — per-library app-private directory tree (`context.filesDir/webdav/<libraryId>/...`), file read/write, plus a Room `SyncFileState` table tracking last-known remote ETag/mtime per path.
3. `service/store/RecipeFileFormat.kt` — `Recipe` ⇄ (YAML front matter + markdown body) serialization; parallel YAML (de)serializers for the `.state/` entities.
4. `service/store/index/` — Room `AppDatabase` + entities/DAOs mirroring the fields repositories currently query (`Recipe`, flattened `RecipeIngredient` rows for the ingredient-autocomplete aggregate query, `ShoppingList`, `ShoppingListItem`, `PlannedRecipe`, `ActiveRecipes`, `RunningTimer`), all Flow-based to preserve today's live-query UX.
5. `service/store/SyncEngine.kt` — orchestrates the diff/pull/push/conflict-copy cycle described above and rebuilds Room index entries from the resulting file set.
6. `service/store/WebDavService.kt` — replaces `CouchbaseService`; facade with `save`/`load`/`observe`/`delete`/`getImage` used by repositories, writing to the local mirror + Room index synchronously and enqueuing a push.
7. `service/store/LibraryManager.kt` — CRUD over configured libraries (personal + shared), each with its own WebDAV URL/credentials, persisted via the existing `AndroidSettings`/SharedPreferences pattern (`service/settings/AndroidSettings.kt`).
8. `service/background/WebDavSyncWork.kt` — replaces `service/background/SyncWork.kt`; `CoroutineWorker` calling `SyncEngine.syncAll()` across configured libraries. `WorkManagerBackgroundService` keeps its existing scheduling logic, just targets this new worker.
9. `coil/LocalFileFetcher.kt` — replaces `coil/CoilBlobFetcher.kt`; reads recipe images from the local mirror file (evaluate whether Coil's built-in file support is sufficient before writing a custom `Fetcher`).
10. `tools/migration/CouchbaseToWebDavMigrator.kt` — one-time importer: reads all documents via the existing `CouchbaseDatabase`/repositories, writes them into the new local-mirror format (as if freshly pulled), then a normal sync pushes everything to the configured WebDAV server. Triggered from a Settings action; leaves the old Couchbase DB untouched as a safety net until the user confirms success.

## Repository & DI Changes

- Repositories (`RecipeRepository`, `ShoppingListRepository`, `TimerRepository`, `ActiveRecipeRepository`, `IngredientRepository` in `service/couchbase/repository/`) keep their existing public method shapes (`save`, `live`, `livePlanned`, `load`, `loadImage`, `startCooking`/`stopCooking`, etc.) but swap Couchbase `QueryBuilder` internals for Room DAO + Flow queries, and gain library-scoping (default: personal library; "all" views union across configured libraries). This preserves the seam already in place: ViewModels depend only on repositories (confirmed via `di/AppModule.kt`), so most UI code is unaffected.
- `di/AppModule.kt`: replace `CouchbaseAccountListener`/`CouchbaseService` singletons with `LibraryManager`, `WebDavClient`, `SyncEngine`, `WebDavService`, and the Room `AppDatabase`/DAOs; repository `single { }` lines stay the same shape, just constructed against the new service.

## Settings / UI Changes

- `res/xml/root_preferences.xml` + `SettingsViewModel`/`SettingsActivity`: replace the single Sync Gateway account name/password fields with WebDAV URL/username/password for the personal library, plus a small add/remove list UI for shared libraries.
- Recipe list/detail: a library indicator and a "Share…" action that moves a recipe into a chosen shared library.
- `MainComposeActivity`'s sync-status indicator (currently reads Couchbase `ReplicatorActivityLevel`) swaps to a simple idle/syncing/error state exposed by `SyncEngine`.

## Build Config

- Add: OkHttp, Room (+ KSP/kapt compiler), `jackson-dataformat-yaml`.
- Remove (final cleanup phase only, after field validation): `com.couchbase.lite:couchbase-lite-android-ktx`, `service/couchbase/**`, `BuildConfig.couchbaseSyncUrl` and its secrets-gradle-plugin entry.
- Keep as-is: Markwon/flexmark (markdown rendering already works off `Recipe.content`), Jackson core, Koin.

## Implementation Order

1. Add new dependencies (OkHttp, Room, Jackson YAML).
2. `WebDavClient` protocol layer, tested against OkHttp `MockWebServer`.
3. File-format serializers (front matter ⇄ `Recipe`, YAML ⇄ other entities) + Room schema for all 6 entity types.
4. `LocalMirror` + `SyncEngine` (diff/pull/push/conflict-copy), with unit tests covering conflict scenarios.
5. `WebDavService` + `LibraryManager` (multi-library config storage).
6. Migrate repositories one at a time, starting with `RecipeRepository`, keeping method signatures stable.
7. Image pipeline: file-backed storage, new Coil fetcher, update `RecipeEditViewModel`'s save path.
8. `WebDavSyncWork` + `WorkManagerBackgroundService` scheduling; sync-on-write triggers.
9. New Settings UI (WebDAV connection + library management) and the recipe "Share…" action.
10. `CouchbaseToWebDavMigrator` + a Settings/first-run entry point to run it.
11. End-to-end verification (below).
12. Remove Couchbase Lite and related code once validated. Skip for now as the user needs to do the migration before.

## Verification

- Unit tests: `WebDavClient` against `MockWebServer`; front-matter round-trip serialization per entity type; `SyncEngine` diff/conflict logic against simulated local/remote file trees.
- Manual: create/edit/delete a recipe with an image in the running app and confirm the resulting files on the configured WebDAV server (e.g. via a desktop WebDAV client or `curl -X PROPFIND`); force a two-device conflict and confirm a conflict-copy file appears with both versions visible; trigger the periodic WorkManager job manually (`adb shell cmd jobscheduler run`) and confirm sync still applies while the app is backgrounded; run the migration tool against real existing Couchbase data and diff recipe/shopping-list counts and content before vs. after.
