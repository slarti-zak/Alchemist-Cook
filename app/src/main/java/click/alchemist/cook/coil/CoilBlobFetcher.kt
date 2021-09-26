package click.alchemist.cook.coil

import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.size.Size
import com.couchbase.lite.Blob
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream

class CoilBlobFetcher : Fetcher<Blob> {
	override suspend fun fetch(pool: BitmapPool, data: Blob, size: Size, options: Options): FetchResult {
		val source = ByteArrayInputStream(data.content).source().buffer()
		return SourceResult(source, data.contentType, DataSource.DISK)
	}

	override fun key(data: Blob): String {
		val digest = data.digest()
		if (digest != null) return digest

		return "${data.contentType}-${data.length()}"
	}
}