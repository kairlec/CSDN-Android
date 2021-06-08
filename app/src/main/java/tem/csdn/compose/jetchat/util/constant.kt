package tem.csdn.compose.jetchat.util

import java.security.MessageDigest

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHexString()