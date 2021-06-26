package tem.csdn.compose.jetchat.util

import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.size.Size
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream

/**
 * coil的字节数组解析器
 * 让coil支持解析字节数组的图片表示
 */
class CoilByteArrayFetcher : Fetcher<ByteArray> {
    override suspend fun fetch(
        pool: BitmapPool,
        data: ByteArray,
        size: Size,
        options: Options
    ): FetchResult {
        return SourceResult(
            source = ByteArrayInputStream(data).source().buffer(),
            mimeType = null,
            dataSource = DataSource.MEMORY
        )
    }

    override fun key(data: ByteArray): String? {
        return null
    }
}