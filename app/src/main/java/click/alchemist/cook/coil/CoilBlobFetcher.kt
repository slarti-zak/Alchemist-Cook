package click.alchemist.cook.coil

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import com.couchbase.lite.Blob
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream

class CoilBlobFetcher(val data: Blob, val context: Context) : Fetcher {
	override suspend fun fetch(): FetchResult {
		val source = ByteArrayInputStream(data.content).source().buffer()
		val imageSource = ImageSource(source, context)
		return SourceResult(imageSource, data.contentType, DataSource.DISK)
	}
}

class CoilBlobFetcherFactory(val context: Context) : Fetcher.Factory<Blob> {
	override fun create(data: Blob, options: Options, imageLoader: ImageLoader): Fetcher {
		return CoilBlobFetcher(data, context)
	}
}

class CoilBlobKeyer : Keyer<Blob> {
	override fun key(data: Blob, options: Options): String {
		return data.digest() ?: "${data.contentType}-${data.length()}"
	}
}