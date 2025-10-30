package io.clroot.ball.domain.exception

class DomainStateException(
    message: String,
    errorType: ErrorType = ErrorType.UNPROCESSABLE,
    val entity: EntityReference? = null,
    code: String = DomainErrorCodes.DOMAIN_STATE_ERROR,
    messageKey: String? = "domain.state.error",
    messageArgs: Map<String, Any?> = emptyMap(),
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : DomainException(
        message = message,
        errorType = errorType,
        errorCode = code,
        messageKey = messageKey,
        messageArgs = messageArgs,
        metadata =
            buildMap {
                putAll(metadata)
                entity?.asMetadata()?.let { putAll(it) }
            },
        cause = cause,
    ) {
    val entityType: String? get() = entity?.typeName
    val entityId: String? get() = entity?.id

    companion object {
        /** 엔티티 없음 */
        inline fun <reified T : Any> notFound(id: Any? = null): DomainStateException {
            val reference = EntityReference(T::class, id?.toString())
            val typeName = reference.typeName
            return DomainStateException(
                message =
                    if (id != null) {
                        "${typeName}을(를) 찾을 수 없습니다: $id"
                    } else {
                        "${typeName}을(를) 찾을 수 없습니다"
                    },
                errorType = ErrorType.NOT_FOUND,
                entity = reference,
                code = DomainErrorCodes.DOMAIN_STATE_NOT_FOUND,
                messageKey = "domain.state.not_found",
                messageArgs =
                    buildMap<String, Any?> {
                        put("entity", typeName)
                        id?.let { put("id", it) }
                    },
            )
        }

        /** 엔티티 중복 */
        inline fun <reified T : Any> exists(id: Any): DomainStateException {
            val reference = EntityReference(T::class, id.toString())
            return DomainStateException(
                message = "${reference.typeName}이(가) 이미 존재합니다: $id",
                errorType = ErrorType.CONFLICT,
                entity = reference,
                code = DomainErrorCodes.DOMAIN_STATE_EXISTS,
                messageKey = "domain.state.already_exists",
                messageArgs = mapOf("entity" to reference.typeName, "id" to id),
            )
        }

        /** 상태 전이 불가 */
        fun transition(
            from: Any,
            to: Any,
        ) = DomainStateException(
            message = "상태를 ${from}에서 $to(으)로 변경할 수 없습니다",
            errorType = ErrorType.UNPROCESSABLE,
            code = DomainErrorCodes.DOMAIN_STATE_INVALID_TRANSITION,
            messageKey = "domain.state.transition_invalid",
            messageArgs = mapOf("from" to from, "to" to to),
        )

        /** 특정 상태 필요 */
        fun requireState(
            state: String,
            action: String,
        ) = DomainStateException(
            message = "$state 상태에서만 $action 가능합니다",
            errorType = ErrorType.PRECONDITION_FAILED,
            code = DomainErrorCodes.DOMAIN_STATE_REQUIRE_STATE,
            messageKey = "domain.state.require_state",
            messageArgs = mapOf("requiredState" to state, "action" to action),
        )

        /** 이미 해당 상태 */
        fun already(state: String) =
            DomainStateException(
                message = "이미 $state 상태입니다",
                errorType = ErrorType.CONFLICT,
                code = DomainErrorCodes.DOMAIN_STATE_ALREADY_STATE,
                messageKey = "domain.state.already_state",
                messageArgs = mapOf("state" to state),
            )
    }

    override fun toString(): String =
        buildString {
            append("DomainStateException(message='").append(message).append("', errorCode='").append(errorCode).append("'")
            entity?.let {
                append(", entityType='").append(it.typeName).append("'")
                it.id?.let { idValue -> append(", entityId='").append(idValue).append("'") }
            }
            append(")")
        }
}
