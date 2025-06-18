# Ball Framework Domain Module

Ball Framework의 핵심 도메인 모듈로, DDD(Domain-Driven Design)와 헥사고날 아키텍처의 기반을 제공합니다.

## 목차
1. [모듈 개요](#모듈-개요)
2. [기본 도메인 모델](#기본-도메인-모델)
3. [값 객체 (Value Objects)](#값-객체-value-objects)
4. [도메인 예외 처리](#도메인-예외-처리)
5. [도메인 이벤트](#도메인-이벤트)
6. [Repository 패턴](#repository-패턴)
7. [명세와 정책 패턴](#명세와-정책-패턴)
8. [페이징 지원](#페이징-지원)
9. [도메인 서비스](#도메인-서비스)
10. [실제 사용 예시](#실제-사용-예시)
11. [베스트 프랙티스](#베스트-프랙티스)

## 모듈 개요

Ball Framework의 Domain 모듈은 다음과 같은 패키지 구조로 구성되어 있습니다:

```
domain/
├── event/           # 도메인 이벤트 시스템
├── exception/       # 도메인 예외 계층
├── model/          # 도메인 모델
│   ├── paging/     # 페이징 관련 기능
│   ├── policy/     # 정책 패턴 구현
│   ├── specification/ # 명세 패턴 구현
│   └── vo/         # 값 객체 (Value Objects)
├── port/           # 헥사고날 아키텍처의 포트 정의
└── service/        # 도메인 서비스
```

## 기본 도메인 모델

### EntityBase

모든 엔티티의 기본 클래스입니다.

```kotlin
abstract class EntityBase<ID : Any>(
    open val id: ID,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null // 소프트 삭제 지원
) {
    // ID 기반 동등성 구현
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}
```

**주요 특징:**
- **소프트 삭제 지원**: `deletedAt` 필드로 논리 삭제
- **ID 기반 동등성**: 같은 ID면 같은 엔티티
- **타임스탬프 자동 관리**: 생성/수정 시간 추적

### AggregateRoot

EntityBase를 확장하여 도메인 이벤트 발행 기능을 제공합니다.

```kotlin
abstract class AggregateRoot<ID : Any>(
    id: ID,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
) : EntityBase<ID>(id, createdAt, updatedAt, deletedAt) {
    
    private val _domainEvents = mutableListOf<DomainEvent>()
    
    val domainEvents: List<DomainEvent> get() = _domainEvents.toList()
    
    protected fun registerEvent(event: DomainEvent) {
        _domainEvents.add(event)
    }
    
    fun clearEvents() {
        _domainEvents.clear()
    }
}
```

**도메인 이벤트 예시:**
```kotlin
class Order(
    id: BinaryId,
    val userId: BinaryId,
    var status: OrderStatus = OrderStatus.PENDING,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt) {
    
    fun confirm() {
        if (status != OrderStatus.PENDING) {
            throw DomainStateException("대기 중인 주문만 확정할 수 있습니다")
        }
        
        status = OrderStatus.CONFIRMED
        registerEvent(OrderConfirmedEvent(id, userId, LocalDateTime.now()))
    }
}
```

## 값 객체 (Value Objects)

### BinaryId - ULID 기반 식별자

타임스탬프 순서를 보장하는 전역 고유 식별자입니다.

```kotlin
@JvmInline
value class BinaryId(val value: ByteArray) {
    companion object {
        fun new(): BinaryId
        fun fromString(value: String): BinaryId
        fun fromBytes(bytes: ByteArray): BinaryId
    }
    
    fun toBytes(): ByteArray
    override fun toString(): String
}
```

**사용 예시:**
```kotlin
// 새로운 ID 생성
val userId = BinaryId.new()

// 문자열에서 변환
val orderId = BinaryId.fromString("01ARZ3NDEKTSV4RRFFQ69G5FAV")

// 데이터베이스 저장 시 바이너리 변환
val binaryData = userId.toBytes()
```

**주요 특징:**
- **성능 최적화**: @JvmInline value class로 오버헤드 최소화
- **타임스탬프 순서**: 생성 시간 순으로 정렬 가능
- **전역 고유성**: 분산 환경에서도 충돌 없음
- **인간 친화적**: Base32 인코딩으로 읽기 쉬운 문자열

### Email - 이메일 주소

유효성 검증이 포함된 이메일 값 객체입니다.

```kotlin
@JvmInline
value class Email(val value: String) {
    init {
        require(EMAIL_REGEX.matches(value)) { "유효하지 않은 이메일 형식입니다: $value" }
    }
    
    companion object {
        fun from(value: String): Email = Email(value)
    }
}
```

**사용 예시:**
```kotlin
// 유효한 이메일
val userEmail = Email("user@example.com")

// 유효성 검증 실패 시 예외 발생
val invalidEmail = Email("invalid-email") // IllegalArgumentException
```

### ValueObject 마커 인터페이스

```kotlin
interface ValueObject {
    // 값 객체임을 나타내는 마커 인터페이스
}
```

**값 객체 구현 예시:**
```kotlin
data class Money(
    val amount: BigDecimal,
    val currency: Currency = Currency.getInstance("KRW")
) : ValueObject {
    
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "통화가 다릅니다" }
        return Money(amount + other.amount, currency)
    }
    
    operator fun times(quantity: Int): Money {
        return Money(amount * quantity.toBigDecimal(), currency)
    }
    
    companion object {
        val ZERO = Money(BigDecimal.ZERO)
    }
}
```

## 도메인 예외 처리

Ball Framework는 타입 안전한 도메인 예외 계층을 제공합니다.

### 예외 계층 구조

```kotlin
// 기본 도메인 예외
abstract class DomainException(message: String) : RuntimeException(message)

// 도메인 검증 실패
class DomainValidationException(
    message: String,
    val field: String? = null,
    val code: String? = null
) : DomainException(message)

// 비즈니스 규칙 위반
class BusinessRuleException(
    message: String,
    val ruleCode: String? = null
) : DomainException(message)

// 도메인 상태 불일치
class DomainStateException(
    message: String,
    val entityType: String? = null,
    val entityId: String? = null
) : DomainException(message)

// 외부 시스템 예외
class ExternalSystemException(
    message: String,
    val systemName: String? = null,
    cause: Throwable? = null
) : DomainException(message)
```

### 팩토리 메서드 활용

```kotlin
// 검증 실패
throw DomainValidationException.fieldValidation("email", "이메일 형식이 잘못되었습니다")
throw DomainValidationException.invalidId("user-123")

// 비즈니스 규칙 위반
throw BusinessRuleException.policyViolation("ORDER_LIMIT", "일일 주문 한도를 초과했습니다")
throw BusinessRuleException.invariantViolation("주문 금액은 0보다 커야 합니다")

// 상태 불일치
throw DomainStateException.entityNotFound(User::class, "user-123")
throw DomainStateException.invalidState(
    Order::class, 
    currentState = "DELIVERED", 
    expectedState = "PENDING"
)
```

### 사용 예시

```kotlin
class User(
    id: BinaryId,
    var name: String,
    var email: Email
) : EntityBase<BinaryId>(id) {
    
    fun changeName(newName: String) {
        if (newName.isBlank()) {
            throw DomainValidationException.fieldValidation(
                "name", 
                "이름은 비어있을 수 없습니다"
            )
        }
        if (newName.length > 50) {
            throw DomainValidationException.fieldValidation(
                "name", 
                "이름은 50자를 초과할 수 없습니다"
            )
        }
        this.name = newName
    }
    
    fun deactivate() {
        if (status == UserStatus.INACTIVE) {
            throw DomainStateException.invalidState(
                User::class,
                currentState = status.name,
                expectedState = UserStatus.ACTIVE.name
            )
        }
        this.status = UserStatus.INACTIVE
    }
}
```

## 도메인 이벤트

Ball Framework는 두 가지 유형의 이벤트를 지원합니다.

### 도메인 이벤트 (DomainEvent)

프로세스 내에서 즉시 처리되는 이벤트입니다.

```kotlin
interface DomainEvent : Event {
    // 도메인 내 즉시 처리
}

data class OrderConfirmedEvent(
    val orderId: BinaryId,
    val userId: BinaryId,
    val totalAmount: Money,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val id: String = BinaryId.new().toString()
    override val type: String = "OrderConfirmed"
}
```

### 통합 이벤트 (IntegrationEvent)

서비스 간 통신을 위한 이벤트입니다.

```kotlin
interface IntegrationEvent : Event {
    val source: String
    val destination: String?
    val correlationId: String
    val metadata: Map<String, Any>
}

data class OrderCompletedIntegrationEvent(
    val orderId: BinaryId,
    val userId: BinaryId,
    override val source: String = "order-service",
    override val destination: String? = null,
    override val correlationId: String = UUID.randomUUID().toString(),
    override val metadata: Map<String, Any> = emptyMap(),
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : IntegrationEvent {
    override val id: String = BinaryId.new().toString()
    override val type: String = "OrderCompleted"
}
```

### 이벤트 처리 패턴

```kotlin
// Repository에서 도메인 이벤트 자동 발행
@Repository
class OrderJpaAdapter(
    private val jpaRepository: OrderJpaRepository,
    private val eventPublisher: ApplicationEventPublisher
) : JpaRepositoryAdapter<Order, BinaryId, OrderJpaRecord>(jpaRepository), OrderRepository {
    
    override fun save(entity: Order): Order {
        val saved = super.save(entity)
        
        // 도메인 이벤트 발행
        entity.domainEvents.forEach { event ->
            eventPublisher.publishEvent(event)
        }
        entity.clearEvents()
        
        return saved
    }
}

// 이벤트 핸들러
@EventListener
class OrderEventHandler {
    
    @EventListener
    fun handleOrderConfirmed(event: OrderConfirmedEvent) {
        // 재고 차감, 결제 처리 등
        log.info("주문 확정됨: ${event.orderId}")
    }
}
```

## Repository 패턴

### 기본 Repository

```kotlin
interface Repository<T : EntityBase<ID>, ID : Any> {
    fun findById(id: ID): T?
    fun findAll(): List<T>
    fun save(entity: T): T
    fun update(id: ID, modifier: (T) -> Unit): T
    fun update(entity: T, modifier: (T) -> Unit): T
    fun delete(entity: T)
    fun delete(id: ID)
}
```

### SpecificationRepository

복잡한 쿼리를 명세 패턴으로 처리합니다.

```kotlin
interface SpecificationRepository<T : EntityBase<ID>, ID : Any> : Repository<T, ID> {
    fun findBySpecification(specification: Specification<T>): List<T>
    fun findOneBySpecification(specification: Specification<T>): T?
    fun existsById(id: ID): Boolean
    fun existsBySpecification(specification: Specification<T>): Boolean
    fun countBySpecification(specification: Specification<T>): Long
}
```

### Repository 구현 예시

```kotlin
interface UserRepository : Repository<User, BinaryId> {
    fun findByEmail(email: Email): User?
    fun existsByEmail(email: Email): Boolean
    fun findAllActive(): List<User>
}

interface OrderRepository : SpecificationRepository<Order, BinaryId> {
    fun findByUserId(userId: BinaryId): List<Order>
    fun findByUserIdAndStatus(userId: BinaryId, status: OrderStatus): List<Order>
    fun countByUserIdAndStatus(userId: BinaryId, status: OrderStatus): Int
}
```

## 명세와 정책 패턴

### Specification (명세 패턴)

비즈니스 조건을 캡슐화하여 재사용 가능한 검증 로직을 만듭니다.

```kotlin
interface Specification<T> {
    fun isSatisfiedBy(t: T): Boolean
    fun and(other: Specification<T>): Specification<T>
    fun or(other: Specification<T>): Specification<T>
    fun not(): Specification<T>
}
```

**사용 예시:**
```kotlin
class AdultUserSpecification : Specification<User> {
    override fun isSatisfiedBy(user: User): Boolean {
        return user.age >= 18
    }
}

class ActiveUserSpecification : Specification<User> {
    override fun isSatisfiedBy(user: User): Boolean {
        return user.status == UserStatus.ACTIVE
    }
}

// 명세 결합
val eligibleUserSpec = AdultUserSpecification()
    .and(ActiveUserSpecification())

// 사용
if (eligibleUserSpec.isSatisfiedBy(user)) {
    // 자격을 만족하는 사용자
}
```

### Policy (정책 패턴)

비즈니스 규칙을 적용하고 위반 시 예외를 발생시킵니다.

```kotlin
interface Policy<T> {
    fun validate(target: T)
    fun isSatisfiedBy(target: T): Boolean
    fun and(other: Policy<T>): Policy<T>
    fun or(other: Policy<T>): Policy<T>
}
```

**사용 예시:**
```kotlin
class OrderLimitPolicy : Policy<User> {
    override fun validate(user: User) {
        if (!isSatisfiedBy(user)) {
            throw BusinessRuleException.policyViolation(
                "ORDER_LIMIT", 
                "일일 주문 한도를 초과했습니다"
            )
        }
    }
    
    override fun isSatisfiedBy(user: User): Boolean {
        return user.todayOrderCount < user.maxDailyOrders
    }
}

// 정책 적용
val orderPolicy = OrderLimitPolicy()
    .and(ActiveUserPolicy())

orderPolicy.validate(user) // 정책 위반 시 예외 발생
```

### Specification을 Policy로 변환

```kotlin
val userSpecification = AdultUserSpecification()
val userPolicy = SpecificationPolicy(
    specification = userSpecification,
    errorMessageProvider = { "성인만 이용 가능합니다" }
)

userPolicy.validate(user) // 미성년자일 경우 예외 발생
```

## 페이징 지원

### PageRequest

```kotlin
data class PageRequest(
    val page: Int = 0,        // 0부터 시작
    val size: Int = 30,       // 기본 페이지 크기
    val sort: Sort = Sort.unsorted()
) {
    val offset: Long get() = page.toLong() * size
}
```

### Sort

```kotlin
class Sort(private val orders: List<Order> = emptyList()) {
    companion object {
        fun by(vararg properties: String): Sort
        fun ascending(property: String): Sort  
        fun descending(property: String): Sort
        fun unsorted(): Sort
    }
    
    fun and(other: Sort): Sort
    fun ascending(): Sort
    fun descending(): Sort
}

data class Order(
    val property: String,
    val direction: Direction = Direction.ASC
) {
    companion object {
        fun asc(property: String): Order
        fun desc(property: String): Order
    }
}

enum class Direction { ASC, DESC }
```

### Page

```kotlin
data class Page<T>(
    val content: List<T>,
    val pageRequest: PageRequest,
    val totalElements: Long
) {
    val totalPages: Int
    val hasNext: Boolean
    val hasPrevious: Boolean
    val isFirst: Boolean
    val isLast: Boolean
    
    fun <R> map(transform: (T) -> R): Page<R>
}
```

**사용 예시:**
```kotlin
// 페이지 요청 생성
val pageRequest = PageRequest(
    page = 0,
    size = 20,
    sort = Sort.by("createdAt").descending()
)

// Repository에서 페이징 조회
interface OrderRepository : SpecificationRepository<Order, BinaryId> {
    fun findByUserId(userId: BinaryId, pageRequest: PageRequest): Page<Order>
}

// 페이지 결과 처리
val orderPage = orderRepository.findByUserId(userId, pageRequest)
println("총 ${orderPage.totalElements}개 중 ${orderPage.content.size}개 조회")
println("다음 페이지 존재: ${orderPage.hasNext}")

// 타입 변환
val orderDtoPage = orderPage.map { order -> OrderDto.from(order) }
```

## 도메인 서비스

도메인 서비스는 특정 엔티티에 속하지 않는 도메인 로직을 캡슐화합니다.

```kotlin
interface DomainService {
    // 마커 인터페이스
}

@Component
class OrderPricingService : DomainService {
    
    fun calculateTotalPrice(
        items: List<OrderItem>, 
        discountPolicy: DiscountPolicy
    ): Money {
        val subtotal = items.fold(Money.ZERO) { acc, item ->
            acc + (item.price * item.quantity)
        }
        
        val discount = discountPolicy.calculateDiscount(subtotal)
        return subtotal - discount
    }
    
    fun validateOrderItems(items: List<OrderItem>) {
        if (items.isEmpty()) {
            throw BusinessRuleException.invariantViolation("주문 항목이 없습니다")
        }
        
        items.forEach { item ->
            if (item.quantity <= 0) {
                throw DomainValidationException.fieldValidation(
                    "quantity", 
                    "수량은 0보다 커야 합니다"
                )
            }
        }
    }
}
```

**사용 예시:**
```kotlin
class Order(
    // ... 생성자
    private val pricingService: OrderPricingService
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt) {
    
    fun addItems(items: List<OrderItem>, discountPolicy: DiscountPolicy) {
        // 도메인 서비스 활용
        pricingService.validateOrderItems(items)
        
        _items.addAll(items)
        totalAmount = pricingService.calculateTotalPrice(_items, discountPolicy)
        
        registerEvent(OrderItemsAddedEvent(id, items.size))
    }
}
```

## 실제 사용 예시

### 도메인 모델 정의

```kotlin
// 사용자 집합체
class User(
    id: BinaryId,
    var name: String,
    var email: Email,
    var status: UserStatus = UserStatus.ACTIVE,
    var age: Int,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt) {
    
    fun changeName(newName: String) {
        if (newName.isBlank()) {
            throw DomainValidationException.fieldValidation("name", "이름은 필수입니다")
        }
        this.name = newName
        registerEvent(UserNameChangedEvent(id, name, newName))
    }
    
    fun changeEmail(newEmail: Email) {
        val oldEmail = this.email
        this.email = newEmail
        registerEvent(UserEmailChangedEvent(id, oldEmail, newEmail))
    }
    
    fun deactivate() {
        if (status != UserStatus.ACTIVE) {
            throw DomainStateException.invalidState(
                User::class, 
                status.name, 
                UserStatus.ACTIVE.name
            )
        }
        status = UserStatus.INACTIVE
        registerEvent(UserDeactivatedEvent(id))
    }
}

// 주문 집합체
class Order(
    id: BinaryId,
    val userId: BinaryId,
    private val _items: MutableList<OrderItem> = mutableListOf(),
    var status: OrderStatus = OrderStatus.PENDING,
    var totalAmount: Money = Money.ZERO,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt) {
    
    val items: List<OrderItem> get() = _items.toList()
    
    fun addItem(productId: BinaryId, productName: String, price: Money, quantity: Int) {
        if (status != OrderStatus.PENDING) {
            throw BusinessRuleException.invariantViolation("확정된 주문은 수정할 수 없습니다")
        }
        
        val item = OrderItem(productId, productName, price, quantity)
        _items.add(item)
        recalculateTotalAmount()
        
        registerEvent(OrderItemAddedEvent(id, productId, quantity))
    }
    
    fun confirm() {
        if (status != OrderStatus.PENDING) {
            throw DomainStateException.invalidState(
                Order::class,
                status.name,
                OrderStatus.PENDING.name
            )
        }
        if (_items.isEmpty()) {
            throw BusinessRuleException.invariantViolation("주문 항목이 없습니다")
        }
        
        status = OrderStatus.CONFIRMED
        registerEvent(OrderConfirmedEvent(id, userId, totalAmount))
    }
    
    private fun recalculateTotalAmount() {
        totalAmount = _items.fold(Money.ZERO) { acc, item ->
            acc + (item.price * item.quantity)
        }
    }
}
```

### UseCase 구현

```kotlin
@Service
@Transactional
class CreateOrderUseCase(
    applicationEventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val orderPolicy: OrderPolicy
) : UseCase<CreateOrderCommand, Order>(applicationEventPublisher) {

    override fun executeInternal(command: CreateOrderCommand): Order {
        // 1. 사용자 조회 및 검증
        val user = userRepository.findById(command.userId)
            ?: throw DomainStateException.entityNotFound(User::class, command.userId.toString())
        
        // 2. 정책 검증
        orderPolicy.validate(user)
        
        // 3. 주문 생성
        val order = Order(
            id = BinaryId.new(),
            userId = user.id
        )
        
        // 4. 주문 항목 추가
        command.items.forEach { item ->
            order.addItem(
                productId = item.productId,
                productName = item.productName,
                price = item.price,
                quantity = item.quantity
            )
        }
        
        // 5. 주문 확정
        order.confirm()
        
        // 6. 저장 (도메인 이벤트 자동 발행)
        return orderRepository.save(order)
    }
}
```

### 명세와 정책 활용

```kotlin
// 주문 가능 사용자 명세
class EligibleUserSpecification : Specification<User> {
    override fun isSatisfiedBy(user: User): Boolean {
        return user.status == UserStatus.ACTIVE && user.age >= 18
    }
}

// 주문 한도 정책
class OrderLimitPolicy(
    private val orderRepository: OrderRepository
) : Policy<User> {
    
    override fun validate(user: User) {
        if (!isSatisfiedBy(user)) {
            throw BusinessRuleException.policyViolation(
                "ORDER_LIMIT",
                "일일 주문 한도를 초과했습니다"
            )
        }
    }
    
    override fun isSatisfiedBy(user: User): Boolean {
        val todayOrders = orderRepository.countByUserIdAndDateRange(
            user.id,
            LocalDate.now().atStartOfDay().toLocalDateTime(ZoneOffset.UTC),
            LocalDate.now().plusDays(1).atStartOfDay().toLocalDateTime(ZoneOffset.UTC)
        )
        return todayOrders < 10 // 일일 최대 10개 주문
    }
}

// 복합 정책
@Component
class OrderPolicy(
    private val orderRepository: OrderRepository
) : Policy<User> {
    
    private val eligibleUserPolicy = SpecificationPolicy(
        EligibleUserSpecification(),
        { "성인 활성 사용자만 주문할 수 있습니다" }
    )
    
    private val orderLimitPolicy = OrderLimitPolicy(orderRepository)
    
    override fun validate(user: User) {
        eligibleUserPolicy.and(orderLimitPolicy).validate(user)
    }
    
    override fun isSatisfiedBy(user: User): Boolean {
        return eligibleUserPolicy.isSatisfiedBy(user) && orderLimitPolicy.isSatisfiedBy(user)
    }
}
```

## 베스트 프랙티스

### 1. 도메인 모델 설계

```kotlin
// ✅ Good: 도메인 로직을 엔티티에 캡슐화
class Order(...) : AggregateRoot<BinaryId>(...) {
    fun cancel(reason: String) {
        validateCanCancel() // 비즈니스 규칙 검증
        this.status = OrderStatus.CANCELLED
        registerEvent(OrderCancelledEvent(id, reason))
    }
    
    private fun validateCanCancel() {
        if (status == OrderStatus.DELIVERED) {
            throw BusinessRuleException.invariantViolation("배송완료된 주문은 취소할 수 없습니다")
        }
    }
}

// ❌ Bad: 서비스에 비즈니스 로직 분산
@Service
class OrderService {
    fun cancelOrder(orderId: BinaryId, reason: String) {
        val order = orderRepository.findById(orderId)
        if (order.status == OrderStatus.DELIVERED) { // 도메인 로직이 서비스에 있음
            throw Exception("배송완료된 주문은 취소할 수 없습니다")
        }
        order.status = OrderStatus.CANCELLED
        orderRepository.save(order)
    }
}
```

### 2. 값 객체 활용

```kotlin
// ✅ Good: 값 객체로 타입 안전성 확보
data class Money(val amount: BigDecimal, val currency: String) : ValueObject {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "통화가 다릅니다" }
        return Money(amount + other.amount, currency)
    }
}

class Product(
    val price: Money // 타입 안전한 가격
)

// ❌ Bad: 원시 타입 사용
class Product(
    val price: BigDecimal // 어떤 통화인지 알 수 없음
)
```

### 3. 도메인 예외 활용

```kotlin
// ✅ Good: 타입별 도메인 예외 사용
fun changeName(newName: String) {
    if (newName.isBlank()) {
        throw DomainValidationException.fieldValidation("name", "이름은 필수입니다")
    }
}

fun cancel() {
    if (status == OrderStatus.DELIVERED) {
        throw BusinessRuleException.invariantViolation("배송완료된 주문은 취소할 수 없습니다")
    }
}

// ❌ Bad: 일반 예외 사용
fun changeName(newName: String) {
    if (newName.isBlank()) {
        throw IllegalArgumentException("이름은 필수입니다") // 처리하기 어려움
    }
}
```

### 4. Repository update 메서드 활용

```kotlin
// ✅ Good: update 메서드로 낙관적 잠금
orderRepository.update(orderId) { order ->
    order.cancel("고객 요청")
}

// ❌ Bad: 오래된 상태로 수정
val order = orderRepository.findById(orderId)
order?.cancel("고객 요청")
orderRepository.save(order!!)
```

### 5. 명세와 정책 분리

```kotlin
// ✅ Good: 명세와 정책 분리
class AdultUserSpecification : Specification<User> {
    override fun isSatisfiedBy(user: User) = user.age >= 18
}

class AdultUserPolicy : Policy<User> {
    private val specification = AdultUserSpecification()
    
    override fun validate(user: User) {
        if (!specification.isSatisfiedBy(user)) {
            throw BusinessRuleException.policyViolation("AGE_LIMIT", "성인만 이용 가능합니다")
        }
    }
}

// ❌ Bad: 검증과 예외 처리 혼재
class UserValidator {
    fun validateAge(user: User): Boolean {
        if (user.age < 18) {
            throw Exception("성인만 이용 가능합니다") // 검증과 예외 처리가 혼재
        }
        return true
    }
}
```

### 6. 도메인 이벤트 활용

```kotlin
// ✅ Good: 도메인 이벤트로 부수효과 분리
class Order(...) : AggregateRoot<BinaryId>(...) {
    fun confirm() {
        status = OrderStatus.CONFIRMED
        registerEvent(OrderConfirmedEvent(id, userId)) // 이벤트로 분리
    }
}

@EventListener
class OrderEventHandler {
    fun handleOrderConfirmed(event: OrderConfirmedEvent) {
        // 재고 차감, 알림 발송 등 부수효과 처리
    }
}

// ❌ Bad: 도메인 로직에 부수효과 포함
class Order(...) {
    fun confirm(inventoryService: InventoryService, notificationService: NotificationService) {
        status = OrderStatus.CONFIRMED
        inventoryService.decreaseStock(items) // 도메인 로직에 인프라 의존성
        notificationService.sendConfirmEmail(userId)
    }
}
```

## 마무리

Ball Framework의 Domain 모듈은 다음과 같은 이점을 제공합니다:

1. **DDD 원칙 준수**: 비즈니스 로직을 도메인 모델에 캡슐화
2. **타입 안전성**: 값 객체와 도메인 예외로 컴파일 타임 안전성 확보
3. **재사용성**: 명세와 정책 패턴으로 비즈니스 규칙 재사용
4. **확장성**: 인터페이스 기반 설계로 유연한 확장
5. **테스트 용이성**: 도메인 로직의 순수성으로 쉬운 단위 테스트
6. **성능 최적화**: ULID와 @JvmInline value class 활용
7. **이벤트 기반 아키텍처**: 도메인 이벤트로 느슨한 결합

이러한 패턴을 통해 복잡한 비즈니스 로직을 안전하고 유지보수하기 쉽게 구현할 수 있습니다.