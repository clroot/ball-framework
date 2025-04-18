package io.clroot.ball.adapter.inbound.messaging.core.config

import io.clroot.ball.adapter.inbound.messaging.core.MessageConsumer
import io.clroot.ball.adapter.inbound.messaging.core.MessageConsumerRegistry
import io.clroot.ball.adapter.inbound.messaging.core.annotation.MessageConsumerComponent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.StringUtils

/**
 * 메시지 소비자 자동 등록 설정
 * @MessageConsumerComponent 애노테이션이 붙은 빈을 찾아 레지스트리에 등록
 */
@Configuration
class MessageConsumerRegistrar : BeanPostProcessor, ApplicationContextAware {
    private lateinit var registry: MessageConsumerRegistry
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var applicationContext: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
        this.registry = applicationContext.getBean(MessageConsumerRegistry::class.java)
    }

    /**
     * 빈 초기화 후 처리
     * @MessageConsumerComponent 애노테이션이 붙은 빈을 찾아 레지스트리에 등록
     *
     * @param bean 빈 객체
     * @param beanName 빈 이름
     * @return 처리된 빈 객체
     */
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is MessageConsumer<*>) {
            val annotation = AnnotationUtils.findAnnotation(bean.javaClass, MessageConsumerComponent::class.java)

            if (annotation != null && annotation.autoRegister) {
                try {
                    // 토픽 이름 결정 (애노테이션에 지정된 값 또는 빈에서 반환한 값)
                    val topic = if (StringUtils.hasText(annotation.topic)) {
                        annotation.topic
                    } else {
                        bean.getTopicName()
                    }

                    // 소비자 등록
                    registry.registerConsumer(bean)
                    log.info("Automatically registered message consumer: {} for topic: {}", beanName, topic)
                } catch (e: Exception) {
                    log.error("Failed to register message consumer: {}", beanName, e)
                }
            }
        }

        return bean
    }
}
