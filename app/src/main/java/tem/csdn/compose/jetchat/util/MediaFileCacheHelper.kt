package tem.csdn.compose.jetchat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest


class MediaFileCacheHelper {
    companion object {
        lateinit var current: MediaFileCacheHelper
            private set
            @get:Composable
            get
        private const val DISK_CACHE_SIZE = 1024 * 1024 * 1024L // 1GB
        private const val DISK_CACHE_SUBDIR = "thumbnails"
        private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
        private fun hashKey(key: String): String {
            return MessageDigest.getInstance("MD5").run {
                update(key.toByteArray())
                digest().toHexString()
            }
        }

        private fun InputStream.transferTo(out: OutputStream): Long {
            var transferred: Long = 0
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (this.read(buffer, 0, DEFAULT_BUFFER_SIZE).also { read = it } >= 0) {
                out.write(buffer, 0, read)
                transferred += read.toLong()
            }
            return transferred
        }
    }

    private val memoryCache: LruCache<String, Bitmap>
    private lateinit var diskLruCache: DiskLruCache
    suspend fun initDiskLruCache(
        context: Context,
        appVersion: Int,
        valueCount: Int = 1
    ) {
        diskLruCache = withContext(Dispatchers.IO) {
            val cacheDir = getDiskCacheDir(context, DISK_CACHE_SUBDIR)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            DiskLruCache.open(
                cacheDir, appVersion, valueCount, DISK_CACHE_SIZE
            )
        }
        current = this
    }

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    suspend fun loadBitmap(imageKey: String, ifNotExist: suspend () -> InputStream): Bitmap {
        return withContext(Dispatchers.IO) {
            val key = hashKey(imageKey)
            memoryCache.get(key) ?: diskLruCache.get(key)?.getInputStream(0)
                ?.let { BitmapFactory.decodeStream(it) }?.apply {
                    addBitmapToCache(key, this)
                    Log.d("CSDN_LRU", "[${imageKey}]load image:${this}")
                } ?: run {
                val bytes = ifNotExist().readBytes()
                writeToDiskLruCache(key, bytes.inputStream())
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size).apply {
                    addBitmapToCache(key, this)
                }
            }
        }
    }

    private fun writeToDiskLruCache(key: String, inputStream: InputStream) {
        val editor = diskLruCache.edit(key)
        if (editor != null) {
            try {
                editor.newOutputStream(0).use {
                    inputStream.transferTo(it)
                }
                editor.commit()
            } catch (e: Throwable) {
                editor.abort()
            }
        }
        diskLruCache.flush()
    }

    private fun addBitmapToCache(key: String, bitmap: Bitmap) {
        if (memoryCache.get(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }

    private fun getDiskCacheDir(context: Context, uniqueName: String): File {
        return File(context.cacheDir.path + File.separator + uniqueName)
    }
}


