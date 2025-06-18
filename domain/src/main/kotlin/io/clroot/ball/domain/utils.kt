@file:Suppress("ktlint:standard:filename")

package io.clroot.ball.domain

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

inline fun <reified T> T.slf4j(): Logger = LoggerFactory.getLogger(T::class.java)

fun Instant.toLocalDateTime() = this.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
