package io.clroot.ball.adapter.outbound.data.access.redis

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import

@AutoConfiguration
@ConditionalOnMissingBean(RedisConfig::class)
@Import(RedisConfig::class, RedisLockProvider::class)
class RedisAdapterAutoConfiguration
