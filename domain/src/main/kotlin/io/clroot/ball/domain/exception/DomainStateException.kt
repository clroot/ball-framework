package io.clroot.ball.domain.exception

class DomainStateException(
    message: String,
    errorType: ErrorType = ErrorType.UNPROCESSABLE,
    val entityType: String? = null,
    val entityId: String? = null,
    cause: Throwable? = null,
) : DomainException(message, errorType, cause) {
    companion object {
        /** 엔티티 없음 */
        inline fun <reified T : Any> notFound(id: Any? = null): DomainStateException {
            val typeName = T::class.simpleName
            return DomainStateException(
                message =
                    if (id != null) {
                        "${typeName}을(를) 찾을 수 없습니다: $id"
                    } else {
                        "${typeName}을(를) 찾을 수 없습니다"
                    },
                errorType = ErrorType.NOT_FOUND,
                entityType = typeName,
                entityId = id?.toString(),
            )
        }

        /** 엔티티 중복 */
        inline fun <reified T : Any> exists(id: Any) =
            DomainStateException(
                message = "${T::class.simpleName}이(가) 이미 존재합니다: $id",
                errorType = ErrorType.CONFLICT,
                entityType = T::class.simpleName,
                entityId = id.toString(),
            )

        /** 상태 전이 불가 */
        fun transition(
            from: Any,
            to: Any,
        ) = DomainStateException(
            message = "상태를 ${from}에서 $to(으)로 변경할 수 없습니다",
            errorType = ErrorType.UNPROCESSABLE,
        )

        /** 특정 상태 필요 */
        fun requireState(
            state: String,
            action: String,
        ) = DomainStateException(
            message = "$state 상태에서만 $action 가능합니다",
            errorType = ErrorType.PRECONDITION_FAILED,
        )

        /** 이미 해당 상태 */
        fun already(state: String) =
            DomainStateException(
                message = "이미 $state 상태입니다",
                errorType = ErrorType.CONFLICT,
            )
    }

    override fun toString(): String = "DomainStateException(message='$message', entityType='$entityType', entityId='$entityId')"
}
