package io.clroot.ball.application

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync

@AutoConfiguration
@EnableRetry
@EnableAsync
class ApplicationLayerAutoConfiguration
