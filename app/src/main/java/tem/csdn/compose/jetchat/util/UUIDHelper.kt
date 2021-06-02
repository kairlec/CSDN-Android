package tem.csdn.compose.jetchat.util

import android.content.Context
import android.os.Environment
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.*


object UUIDHelper {
    private const val FILE_NAME = ".csdn.uuid"

    operator fun get(context: Context, createIfNotExist: Boolean = true): String {
        val file = context.filesDir.toPath().resolve(FILE_NAME).toFile()
        return if (!file.exists()) {
            if (createIfNotExist) {
                file.createNewFile()
                val uuid = UUID.randomUUID().toString()
                file.writeText(uuid)
                uuid
            } else {
                ""
            }
        } else {
            file.readText()
        }
    }

    operator fun set(context: Context, uuid: String) {
        val file = context.filesDir.toPath().resolve(FILE_NAME).toFile()
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(uuid)
    }

}