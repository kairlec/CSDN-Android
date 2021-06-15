@file:Suppress("DEPRECATION")

package tem.csdn.compose.jetchat.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import java.security.MessageDigest
import java.util.UUID

/**
 * 获取手机的唯一标识ID
 */
@SuppressLint("HardwareIds")
object DeviceIdUtil {
    fun getDeviceId(context: Context): String? {
        val sbDeviceId = StringBuilder()
        val imei = getIMEI(context)
        val androidId = getAndroidId(context)
        val serial = getSerial()
        val uuid = getDeviceUUID()

        //附加imei
        if (!imei.isNullOrBlank()) {
            sbDeviceId.append(imei)
            sbDeviceId.append("|")
        }
        //附加androidId
        if (!androidId.isNullOrBlank()) {
            sbDeviceId.append(androidId)
            sbDeviceId.append("|")
        }
        //附加serial
        if (!serial.isNullOrBlank()) {
            sbDeviceId.append(serial)
            sbDeviceId.append("|")
        }
        //附加uuid
        if (uuid.isNotBlank()) {
            sbDeviceId.append(uuid)
        }
        if (sbDeviceId.isNotEmpty()) {
            try {
                val md5 = sbDeviceId.toString().md5().toHexString()
                if (md5.isNotEmpty()) {
                    //返回最终的DeviceId
                    return md5.formatToUUID()
                }
            } catch (e: Exception) {
                Log.d("CSDN_ERROR", "Get Device Id Error:${e.localizedMessage}", e)
            }
        }
        return null
    }

    /**
     * 取 SHA1
     *
     * @return 对应的Hash值
     */
    private fun String.sha1(): ByteArray {
        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA1")
        messageDigest.reset()
        messageDigest.update(this.toByteArray(charset("UTF-8")))
        return messageDigest.digest()
    }

    /**
     * 取 MD5
     *
     * @return 对应的Hash值
     */
    private fun String.md5(): ByteArray {
        val messageDigest: MessageDigest = MessageDigest.getInstance("MD5")
        messageDigest.reset()
        messageDigest.update(this.toByteArray(charset("UTF-8")))
        return messageDigest.digest()
    }

    /**
     * 把32位长度的格式化为UUID(36位)
     *
     * @return UUID格式
     */
    private fun String.formatToUUID() =
        "${substring(0, 8)}-${substring(8, 12)}-${substring(12, 16)}-${
            substring(
                16,
                20
            )
        }-${substring(20, 32)}"

    /**
     * 获取硬件的UUID
     *
     * @return
     */
    private fun getDeviceUUID(): String {
        val deviceId = "9527" + Build.ID +
                Build.DEVICE +
                Build.BOARD +
                Build.BRAND +
                Build.HARDWARE +
                Build.PRODUCT +
                Build.MODEL +
                Build.SERIAL
        return UUID(deviceId.hashCode().toLong(), Build.SERIAL.hashCode().toLong()).toString()
    }

    private fun getSerial(): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Build.getSerial()
            }
        } catch (e: Exception) {
            Log.d("CSDN_ERROR", "Get Serial Error:${e.localizedMessage}", e)
        }
        return null
    }

    /**
     * 获取AndroidId
     *
     * @param context 上下文
     * @return AndroidId
     */
    private fun getAndroidId(context: Context): String? {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (e: Exception) {
            Log.d("CSDN_ERROR", "Get Android ID Error:${e.localizedMessage}", e)
            null
        }
    }

    /**
     * 获取IMEI
     *
     * @param context 上下文
     * @return IMEI
     */
    private fun getIMEI(context: Context): String? {
        return try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.deviceId
        } catch (e: Exception) {
            Log.d("CSDN_ERROR", "Get IMEI(telephonyManager.getDeviceId) Error:${e.localizedMessage}", e)
            null
        }
    }
}