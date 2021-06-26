package tem.csdn.compose.jetchat.util

import android.content.Context
import android.util.Log
import okhttp3.Cache
import okhttp3.HttpUrl
import java.io.File

//region 待定 HTTP缓存辅助器
/**
 * OkHttp的缓存辅助器
 * 如果缓存里面已经有了,则不进行HTTP请求,而是直接使用已存在的缓存
 * 由于这个OkHttp是Coil所用,且用的是服务器图片缓存
 * 图片没有有效期,所以图片需要使用强制缓存,若存在则绝对不进行HTTP请求,减少服务器压力
 */
object OkHttpCacheHelper {
    const val CACHE_DIR_NAME = "image_cache"

    /**
     * 获取缓存目录
     */
    fun getCacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    }

    /**
     * 获取缓存
     */
    fun getCache(context: Context): Cache {
        return Cache(getCacheDir(context), 1024L * 1024L * 1024L)
    }

    /**
     * 尝试获取缓存过的文件
     * 若不存在返回null
     */
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

    /**
     * 获取缓存文件(若存在)或者是直接返回URL
     * 如果是返回的文件,则coil会自动识别并读取,如果是URL,coil会自动调用OkHttp发送请求
     */
    fun getCacheFileOrUrl(context: Context, url: String): Any {
        return getCacheFile(context, url) ?: url.apply {
            Log.d(
                "CSDN_DEBUG",
                "url=${url} not exists return url"
            )
        }
    }
}

//endregion