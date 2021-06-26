package tem.csdn.compose.jetchat.model

sealed class HeartBeatException(override val message: String? = null, override val cause: Throwable? = null) :
    Exception(message, cause) {
    /**
     * 心跳超时
     */
    class HeartBeatTimeoutException(
        override val message: String = "HeartBeat timeout",
        override val cause: Throwable? = null
    ) :
        HeartBeatException(message, cause)

    /**
     * 心跳内容不匹配
     */
    class HeartBeatContentMismatchException(
        sendContent: String,
        receiveContent: String,
        override val message: String = "HeartBeat last content is '$sendContent' but receive $receiveContent",
        override val cause: Throwable? = null
    ) :
        HeartBeatException(message, cause)
}