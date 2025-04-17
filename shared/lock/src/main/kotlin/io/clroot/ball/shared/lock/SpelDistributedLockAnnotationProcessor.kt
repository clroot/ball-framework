package io.clroot.ball.shared.lock

import io.clroot.ball.shared.lock.exception.LockKeyResolutionException
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

/**
 * SpEL 기반 분산 락 애노테이션 처리기
 */
@Component
class SpelDistributedLockAnnotationProcessor : DistributedLockAnnotationProcessor {
    private val parser: ExpressionParser = SpelExpressionParser()

    override fun resolveKey(annotation: DistributedLock, args: Array<Any?>, parameterNames: Array<String>): String {
        val context = StandardEvaluationContext()

        // 파라미터 이름과 값을 컨텍스트에 등록
        for (i in parameterNames.indices) {
            context.setVariable(parameterNames[i], args.getOrNull(i))
        }

        // SpEL 표현식 평가
        val expression = parser.parseExpression(annotation.key)
        return expression.getValue(context, String::class.java)
            ?: throw LockKeyResolutionException("Lock key expression evaluated to null")
    }
}
