@file:Suppress("ktlint:standard:filename")

package io.clroot.ball.domain

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

inline fun <reified T> T.slf4j(): Logger = LoggerFactory.getLogger(T::class.java)

fun LocalDateTime.toLocalDateTime() = this.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()

fun String.wrapSingleQuotes(): String = "'$this'"

fun String.wrapDoubleQuotes(): String = "\"$this\""

fun Boolean.toYN(): String = if (this) "Y" else "N"

fun String.ynToBoolean(): Boolean = this == "Y"
