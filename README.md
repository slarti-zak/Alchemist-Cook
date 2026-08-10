# Alchemist-Cook

This is a cooking recipe management app for Android made with Compose. The main feature is the management of recipes and shopping lists.

## Recipes

Recipes instructions are either written using markdown or as a special dependency graph. Recipe may contain ingredients which can be directly added to a shopping list.

### Dependency Graph

Steps in a recipe may be written as dependencies on other steps (Do "A" before "B"). When cooking a recipe this graph shows what steps are next on the list. A future feature is tracking how much time each step takes.

## Shopping Lists

Simple "Todo" style list of ingredients with their amount. Ingredients of recipes are suggested when adding new items.

## Sync

The app functions 100 % locally but may sync to a webdav directory to allow for sharing and working on a library with multiple users.

### Nextcloud

Login via Nextcloud is supported, so no app password has to be generated manually.

## History

This project began as a hobby/learning project. It started as a Xamarin/C# Project many years ago. It was then rebuild in native Android xml layouts and finally rebuild in Android Compose. Note that not all history is kept in this git.

In the past it did feature a sync via Couchbase/Couchbase Sync Gateway. It did work and allowed for instant sync between devices but the overhead in managing a Couchbase for such a simple app did not feel right. I thought about migrating to a Cloud Solution but ultimately decided to use webdav as a sync target as I was already using a Nextcloud instance and allows for easy backups.

## AI

This project is a meant as a learning experience. While most of it is done "by hand" as it predates the LLMs of today, it is also a way for me to test LLM features. Areas where AI was used to support the development process:

- Migrating from Couchbase to webdav
- Fixing deprecations in gradle and android lifts
- I18N fixes and texts
