package io.clroot.ball.adapter.inbound.messaging.core

/**
 * 메시지 디스패치 과정에서 발생하는 예외
 *
 * @property messageId 메시지 ID
 * @property topic 메시지 토픽
 * @property retryable 재시도 가능 여부
 */
open class MessageDispatchException(
    message: String,
    cause: Throwable? = null,
    val messageId: String? = null,
    val topic: String? = null,
    val retryable: Boolean = true
) : RuntimeException(buildDetailedMessage(message, messageId, topic, retryable), cause) {

    /**
     * 재시도 불가능한 예외 생성
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     * @param messageId 메시지 ID
     * @param topic 메시지 토픽
     * @return 재시도 불가능한 MessageDispatchException 인스턴스
     */
    companion object {
        /**
         * 메시지 ID와 토픽 정보를 포함한 상세 메시지 생성
         */
        private fun buildDetailedMessage(
            message: String,
            messageId: String?,
            topic: String?,
            retryable: Boolean
        ): String {
            val details = mutableListOf<String>()

            messageId?.let { details.add("messageId=$it") }
            topic?.let { details.add("topic=$it") }
            details.add("retryable=$retryable")

            return if (details.isNotEmpty()) {
                "$message [${details.joinToString(", ")}]"
            } else {
                message
            }
        }

        fun nonRetryable(
            message: String,
            cause: Throwable? = null,
            messageId: String? = null,
            topic: String? = null
        ): MessageDispatchException {
            return MessageDispatchException(
                message = message,
                cause = cause,
                messageId = messageId,
                topic = topic,
                retryable = false
            )
        }
    }
}
