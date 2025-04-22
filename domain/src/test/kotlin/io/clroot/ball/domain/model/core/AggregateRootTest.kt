package io.clroot.ball.domain.model.core

import io.clroot.ball.domain.event.DomainEventBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AggregateRootTest {
    
    @Test
    fun `should register domain events`() {
        // given
        val testAggregate = TestAggregate("test-id")
        val event = TestEvent()
        
        // when
        testAggregate.addItem("item1")
        
        // then
        assertEquals(1, testAggregate.domainEvents.size)
        assertEquals("item1", (testAggregate.domainEvents[0] as TestEvent).itemName)
    }
    
    @Test
    fun `should clear domain events`() {
        // given
        val testAggregate = TestAggregate("test-id")
        
        // when
        testAggregate.addItem("item1")
        assertEquals(1, testAggregate.domainEvents.size)
        
        testAggregate.clearEvents()
        
        // then
        assertTrue(testAggregate.domainEvents.isEmpty())
    }
    
    // 테스트용 집합체 루트 구현
    private class TestAggregate(id: String) : AggregateRoot<String>(id) {
        private val items = mutableListOf<String>()
        
        fun addItem(itemName: String) {
            items.add(itemName)
            registerEvent(TestEvent(itemName))
        }
    }
    
    // 테스트용 도메인 이벤트 구현
    private class TestEvent(val itemName: String = "") : DomainEventBase()
}