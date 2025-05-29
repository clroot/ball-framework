package io.clroot.ball.user.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Import

@AutoConfiguration
@ConditionalOnClass(BallUserConfiguration::class)
@Import(
    BallUserConfiguration::class
)
class BallUserAutoConfiguration