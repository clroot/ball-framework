package io.clroot.ball.domain.model.vo

sealed class IdError {
    data object InvalidIdError : IdError()
}