package io.clroot.ball.shared.lock

interface DistributedLockAnnotationProcessor {
    /**
     * 메서드에 적용된 분산 락 애노테이션 처리
     *
     * @param annotation 락 애노테이션
     * @param args 메서드 인자 배열
     * @param parameterNames 메서드 파라미터 이름 배열
     * @return 실제 락 키
     */
    fun resolveKey(annotation: DistributedLock, args: Array<Any?>, parameterNames: Array<String>): String
}
