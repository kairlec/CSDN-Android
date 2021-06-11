package tem.csdn.compose.jetchat.util

import android.content.Context
import android.util.Log
import okhttp3.Cache
import okhttp3.HttpUrl
import java.io.File

object OkHttpCacheHelper {
    const val CACHE_DIR_NAME = "image_cache"
    fun getCacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    }

    fun getCache(context: Context): Cache {
        return Cache(getCacheDir(context), 1024L * 1024L * 1024L)
    }

    fun getCacheFile(context: Context, url: String): File? {
        val key = Cache.key(HttpUrl.get(url))
        // 在okhttp的disklrucache行为中,.0是记录的http请求,.1是response
        val filename = "${key}.1"
        val cacheFile = File(getCacheDir(context), filename)
        Log.d(
            "CSDN_DEBUG",
            "url=${url} ,filename=${filename},path=${cacheFile.absolutePath},exists=${cacheFile.exists()}"
        )
        return if (cacheFile.exists()) {
            cacheFile
        } else {
            null
        }
    }

    fun getCacheFileOrUrl(context: Context, url: String): Any {
        return getCacheFile(context, url) ?: url.apply {
            Log.d(
                "CSDN_DEBUG",
                "url=${url} not exists return url"
            )
        }
    }
}
