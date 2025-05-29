package io.clroot.example.project.handler

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandler
import io.clroot.example.project.events.UserPasswordChangedEvent
import io.clroot.example.project.events.UserProfileUpdatedEvent
import io.clroot.example.project.events.UserStatusChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 도메인 이벤트 핸들러들
 * 
 * 새로운 @EventHandler 어노테이션을 사용하여 타입 안전한 이벤트 처리를 수행합니다.
 * 이들은 같은 프로세스 내에서 즉시 실행됩니다.
 */
@Component
class UserDomainEventHandlers {
    
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 사용자 비밀번호 변경 이벤트 처리
     * 
     * 비밀번호가 변경되었을 때 다음과 같은 도메인 로직을 수행합니다:
     * - 비밀번호 히스토리 저장
     * - 활성 세션 무효화
     * - 보안 이벤트 로깅
     */
    @EventHandler  // 🔥 새로운 타입 안전한 EventHandler 어노테이션
    fun handleUserPasswordChanged(event: UserPasswordChangedEvent) {
        log.info("Processing password change event for user: {}", event.userId)
        
        try {
            // 1. 비밀번호 히스토리 저장
            savePasswordHistory(event.userId, event.changedAt)
            
            // 2. 활성 세션 무효화 (보안상 중요)
            invalidateUserSessions(event.userId)
            
            // 3. 보안 이벤트 로깅
            logSecurityEvent(
                userId = event.userId,
                action = "PASSWORD_CHANGED",
                isAdminAction = event.isAdminAction,
                timestamp = event.changedAt
            )
            
            // 4. 알림 발송 (필요시)
            if (!event.isAdminAction) {
                sendPasswordChangeNotification(event.userId)
            }
            
            log.info("Password change event processed successfully for user: {}", event.userId)
            
        } catch (e: Exception) {
            log.error("Failed to process password change event for user: {}", event.userId, e)
            // 도메인 이벤트 처리 실패 시 복구 로직 또는 알림
            handleEventProcessingError(event, e)
        }
    }

    /**
     * 사용자 프로필 업데이트 이벤트 처리
     * 
     * 프로필이 업데이트되었을 때 다음과 같은 도메인 로직을 수행합니다:
     * - 변경 이력 저장
     * - 캐시 무효화
     * - 검색 인덱스 업데이트
     */
    @EventHandler  // 🔥 타입 안전한 이벤트 핸들러
    fun handleUserProfileUpdated(event: UserProfileUpdatedEvent) {
        log.info("Processing profile update event for user: {}", event.userId)
        
        try {
            // 1. 변경 이력 저장
            saveProfileChangeHistory(
                userId = event.userId,
                changedFields = event.changedFields,
                previousValues = event.previousValues,
                changedAt = event.occurredAt
            )
            
            // 2. 사용자 캐시 무효화
            invalidateUserCache(event.userId)
            
            // 3. 검색 인덱스 업데이트 (사용자 검색 기능이 있는 경우)
            updateUserSearchIndex(event.userId, event.changedFields)
            
            // 4. 중요한 정보 변경 시 알림
            if (event.changedFields.containsKey("email")) {
                sendProfileChangeNotification(event.userId, "email")
            }
            
            log.info("Profile update event processed successfully for user: {}", event.userId)
            
        } catch (e: Exception) {
            log.error("Failed to process profile update event for user: {}", event.userId, e)
            handleEventProcessingError(event, e)
        }
    }

    /**
     * 사용자 상태 변경 이벤트 처리
     * 
     * 사용자 상태가 변경되었을 때 다음과 같은 도메인 로직을 수행합니다:
     * - 상태 변경 이력 저장
     * - 상태별 후처리 작업
     */
    @EventHandler  // 🔥 타입 안전한 이벤트 핸들러
    fun handleUserStatusChanged(event: UserStatusChangedEvent) {
        log.info("Processing status change event for user: {} ({} -> {})", 
            event.userId, event.fromStatus, event.toStatus)
        
        try {
            // 1. 상태 변경 이력 저장
            saveStatusChangeHistory(
                userId = event.userId,
                fromStatus = event.fromStatus,
                toStatus = event.toStatus,
                reason = event.reason,
                changedAt = event.occurredAt
            )
            
            // 2. 상태별 후처리 작업
            when (event.toStatus) {
                "LOCKED" -> {
                    invalidateUserSessions(event.userId)
                    sendAccountLockedNotification(event.userId)
                }
                "INACTIVE" -> {
                    invalidateUserSessions(event.userId)
                    scheduleDataCleanup(event.userId)
                }
                "ACTIVE" -> {
                    sendAccountActivatedNotification(event.userId)
                }
            }
            
            log.info("Status change event processed successfully for user: {}", event.userId)
            
        } catch (e: Exception) {
            log.error("Failed to process status change event for user: {}", event.userId, e)
            handleEventProcessingError(event, e)
        }
    }

    // Private helper methods (동일한 구현)
    private fun savePasswordHistory(userId: String, changedAt: java.time.Instant) {
        log.debug("Saving password history for user: {}", userId)
        // 실제로는 PasswordHistoryRepository.save() 등을 사용
    }

    private fun invalidateUserSessions(userId: String) {
        log.debug("Invalidating sessions for user: {}", userId)
        // 실제로는 SessionManager.invalidateByUserId() 등을 사용
    }

    private fun logSecurityEvent(userId: String, action: String, isAdminAction: Boolean, timestamp: java.time.Instant) {
        log.info("SECURITY_EVENT: user={}, action={}, adminAction={}, timestamp={}", 
            userId, action, isAdminAction, timestamp)
        // 실제로는 SecurityAuditRepository.save() 등을 사용
    }

    private fun sendPasswordChangeNotification(userId: String) {
        log.debug("Sending password change notification to user: {}", userId)
        // 실제로는 NotificationService.sendPasswordChangeAlert() 등을 사용
    }

    private fun saveProfileChangeHistory(
        userId: String, 
        changedFields: Map<String, Any?>,
        previousValues: Map<String, Any?>,
        changedAt: java.time.Instant
    ) {
        log.debug("Saving profile change history for user: {}, fields: {}", userId, changedFields.keys)
        // 실제로는 ProfileHistoryRepository.save() 등을 사용
    }

    private fun invalidateUserCache(userId: String) {
        log.debug("Invalidating cache for user: {}", userId)
        // 실제로는 CacheManager.evict("users", userId) 등을 사용
    }

    private fun updateUserSearchIndex(userId: String, changedFields: Map<String, Any?>) {
        log.debug("Updating search index for user: {}, fields: {}", userId, changedFields.keys)
        // 실제로는 ElasticSearchService.updateUser() 등을 사용
    }

    private fun sendProfileChangeNotification(userId: String, fieldName: String) {
        log.debug("Sending profile change notification to user: {}, field: {}", userId, fieldName)
        // 실제로는 NotificationService.sendProfileChangeAlert() 등을 사용
    }

    private fun saveStatusChangeHistory(
        userId: String, 
        fromStatus: String, 
        toStatus: String, 
        reason: String?, 
        changedAt: java.time.Instant
    ) {
        log.debug("Saving status change history for user: {} ({} -> {})", userId, fromStatus, toStatus)
        // 실제로는 StatusHistoryRepository.save() 등을 사용
    }

    private fun sendAccountLockedNotification(userId: String) {
        log.debug("Sending account locked notification to user: {}", userId)
        // 실제로는 NotificationService.sendAccountLockedAlert() 등을 사용
    }

    private fun scheduleDataCleanup(userId: String) {
        log.debug("Scheduling data cleanup for inactive user: {}", userId)
        // 실제로는 DataCleanupScheduler.schedule() 등을 사용
    }

    private fun sendAccountActivatedNotification(userId: String) {
        log.debug("Sending account activated notification to user: {}", userId)
        // 실제로는 NotificationService.sendAccountActivatedAlert() 등을 사용
    }

    private fun handleEventProcessingError(event: io.clroot.ball.domain.event.DomainEvent, error: Exception) {
        log.error("Event processing failed: event={}, error={}", event.type, error.message)
        // 실제로는 ErrorNotificationService.sendAlert() 등을 사용
        // 또는 재시도 큐에 이벤트를 다시 넣기
    }
}
