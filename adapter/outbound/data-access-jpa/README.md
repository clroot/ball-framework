# Ball Framework JPA Data Access Module

Ball Framework의 JPA 데이터 액세스 모듈로, 도메인 모델과 JPA 엔티티 간의 매핑을 지원합니다.

## 목차
1. [모듈 개요](#모듈-개요)
2. [기본 Record 클래스](#기본-record-클래스)
3. [커스텀 ID 타입 사용법](#커스텀-id-타입-사용법)
4. [JPA 어댑터 구현](#jpa-어댑터-구현)
5. [사용 예시](#사용-예시)

## 모듈 개요

이 모듈은 도메인 모델과 JPA 엔티티 간의 매핑을 위한 기반 클래스들을 제공합니다. 
**ID 타입에 의존하지 않는 제네릭 구조**로 설계되어, 다양한 도메인 특화 ID 타입을 유연하게 지원합니다.

### 핵심 특징

- **제네릭 기반**: 어떤 ID 타입도 사용 가능 (BinaryId, UserId, OrderId 등)
- **타입 안전성**: 컴파일 타임에 타입 매칭 검증
- **낙관적 잠금**: AggregateRoot용 버전 관리 내장
- **소프트 삭제**: deletedAt 필드 지원
- **자동 타임스탬프**: 생성/수정 시간 자동 관리

## 기본 Record 클래스

### EntityRecord<E, ID>

모든 JPA 엔티티 레코드의 기본 클래스입니다.

```kotlin
@MappedSuperclass
abstract class EntityRecord<E : EntityBase<ID>, ID : Any>(
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?
) : DataModel<E> {
    
    @CreationTimestamp
    var createdAt: Instant = createdAt
        protected set

    @UpdateTimestamp
    var updatedAt: Instant = updatedAt
        protected set

    var deletedAt: Instant? = deletedAt
        protected set
}
```

### AggregateRootRecord<E, ID>

집합체 루트를 위한 JPA 레코드 클래스로, 낙관적 잠금을 지원합니다.

```kotlin
@MappedSuperclass
abstract class AggregateRootRecord<E : AggregateRoot<ID>, ID : Any>(
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
    version: Long
) : EntityRecord<E, ID>(createdAt, updatedAt, deletedAt) {
    
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = version
        protected set
}
```

## 커스텀 ID 타입 사용법

### 1. 도메인 ID 타입 정의

```kotlin
// 사용자 ID
@JvmInline
value class UserId(val value: String) {
    companion object {
        fun new(): UserId = UserId(UUID.randomUUID().toString())
        fun from(value: String): UserId = UserId(value)
    }
}

// 주문 ID  
@JvmInline
value class OrderId(val value: BinaryId) {
    companion object {
        fun new(): OrderId = OrderId(BinaryId.new())
        fun from(binaryId: BinaryId): OrderId = OrderId(binaryId)
    }
}
```

### 2. JPA 컨버터 정의

```kotlin
@Converter(autoApply = true)
class UserIdConverter : AttributeConverter<UserId, String> {
    override fun convertToDatabaseColumn(attribute: UserId?): String? {
        return attribute?.value
    }

    override fun convertToEntityAttribute(dbData: String?): UserId? {
        return dbData?.let { UserId.from(it) }
    }
}

@Converter(autoApply = true)
class OrderIdConverter : AttributeConverter<OrderId, ByteArray> {
    override fun convertToDatabaseColumn(attribute: OrderId?): ByteArray? {
        return attribute?.value?.toBytes()
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): OrderId? {
        return dbData?.let { OrderId.from(BinaryId.fromBytes(it)) }
    }
}
```

### 3. JPA Entity Record 구현

```kotlin
@Entity
@Table(name = "users")
class UserJpaRecord(
    @Id
    @Convert(converter = UserIdConverter::class)
    @Column(name = "id", nullable = false)
    var id: UserId,
    
    @Column(name = "name", nullable = false)
    var name: String,
    
    @Column(name = "email", nullable = false)
    var email: String,
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: UserStatus,
    
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?
) : EntityRecord<User, UserId>(createdAt, updatedAt, deletedAt) {
    
    constructor(entity: User) : this(
        id = entity.id,
        name = entity.name,
        email = entity.email.value,
        status = entity.status,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt
    )
    
    override fun toDomain(): User {
        return User(
            id = id,
            name = name,
            email = Email(email),
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }
    
    override fun update(entity: User) {
        this.name = entity.name
        this.email = entity.email.value
        this.status = entity.status
        updateCommonFields(entity)
    }
}

@Entity
@Table(name = "orders")
class OrderJpaRecord(
    @Id
    @Convert(converter = OrderIdConverter::class)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    var id: OrderId,
    
    @Convert(converter = UserIdConverter::class)
    @Column(name = "user_id", nullable = false)
    var userId: UserId,
    
    @ElementCollection
    @CollectionTable(
        name = "order_items",
        joinColumns = [JoinColumn(name = "order_id")]
    )
    var items: MutableList<OrderItemEmbeddable>,
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus,
    
    @Embedded
    var totalAmount: MoneyEmbeddable,
    
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
    version: Long
) : AggregateRootRecord<Order, OrderId>(createdAt, updatedAt, deletedAt, version) {
    
    constructor(entity: Order, version: Long = 0L) : this(
        id = entity.id,
        userId = entity.userId,
        items = entity.items.map { OrderItemEmbeddable(it) }.toMutableList(),
        status = entity.status,
        totalAmount = MoneyEmbeddable(entity.totalAmount),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
        version = version
    )
    
    override fun toDomain(): Order {
        return Order(
            id = id,
            userId = userId,
            _items = items.map { it.toDomain() }.toMutableList(),
            status = status,
            totalAmount = totalAmount.toDomain(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }
    
    override fun update(entity: Order) {
        this.userId = entity.userId
        this.items = entity.items.map { OrderItemEmbeddable(it) }.toMutableList()
        this.status = entity.status
        this.totalAmount = MoneyEmbeddable(entity.totalAmount)
        updateCommonFields(entity)
    }
}
```

## JPA 어댑터 구현

### JpaRepositoryAdapter 기본 클래스 활용

```kotlin
@Repository
class UserJpaAdapter(
    private val jpaRepository: UserJpaRepository
) : JpaRepositoryAdapter<User, UserId, UserJpaRecord>(jpaRepository), UserRepository {
    
    override fun findByEmail(email: Email): User? {
        return jpaRepository.findByEmail(email.value)?.toDomain()
    }
    
    override fun existsByEmail(email: Email): Boolean {
        return jpaRepository.existsByEmail(email.value)
    }
    
    override fun findAllActive(): List<User> {
        return jpaRepository.findByStatus(UserStatus.ACTIVE)
            .map { it.toDomain() }
    }
}

@Repository
class OrderJpaAdapter(
    private val jpaRepository: OrderJpaRepository,
    private val eventPublisher: ApplicationEventPublisher
) : JpaRepositoryAdapter<Order, OrderId, OrderJpaRecord>(jpaRepository), OrderRepository {
    
    override fun save(entity: Order): Order {
        val saved = super.save(entity)
        
        // 도메인 이벤트 발행
        entity.domainEvents.forEach { event ->
            eventPublisher.publishEvent(event)
        }
        entity.clearEvents()
        
        return saved
    }
    
    override fun findByUserId(userId: UserId): List<Order> {
        return jpaRepository.findByUserId(userId)
            .map { it.toDomain() }
    }
}
```

### Spring Data JPA Repository

```kotlin
interface UserJpaRepository : JpaRepository<UserJpaRecord, UserId> {
    fun findByEmail(email: String): UserJpaRecord?
    fun existsByEmail(email: String): Boolean
    fun findByStatus(status: UserStatus): List<UserJpaRecord>
}

interface OrderJpaRepository : JpaRepository<OrderJpaRecord, OrderId> {
    fun findByUserId(userId: UserId): List<OrderJpaRecord>
    fun findByUserIdAndStatus(userId: UserId, status: OrderStatus): List<OrderJpaRecord>
    fun countByUserIdAndStatus(userId: UserId, status: OrderStatus): Int
}
```

## 사용 예시

### 도메인 모델 정의

```kotlin
// 사용자 엔티티 (일반 엔티티)
class User(
    id: UserId,
    var name: String,
    var email: Email,
    var status: UserStatus = UserStatus.ACTIVE,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    deletedAt: Instant? = null
) : EntityBase<UserId>(id, createdAt, updatedAt, deletedAt) {
    
    fun changeName(newName: String) {
        require(newName.isNotBlank()) { "이름은 비어있을 수 없습니다" }
        this.name = newName
    }
    
    fun changeEmail(newEmail: Email) {
        this.email = newEmail
    }
    
    fun deactivate() {
        if (status != UserStatus.ACTIVE) {
            throw DomainStateException("이미 비활성화된 사용자입니다")
        }
        this.status = UserStatus.INACTIVE
    }
}

// 주문 집합체 (AggregateRoot)
class Order(
    id: OrderId,
    val userId: UserId,
    private val _items: MutableList<OrderItem> = mutableListOf(),
    var status: OrderStatus = OrderStatus.PENDING,
    var totalAmount: Money = Money.ZERO,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    deletedAt: Instant? = null
) : AggregateRoot<OrderId>(id, createdAt, updatedAt, deletedAt) {
    
    val items: List<OrderItem> get() = _items.toList()
    
    fun addItem(productId: ProductId, productName: String, price: Money, quantity: Int) {
        if (status != OrderStatus.PENDING) {
            throw BusinessRuleException("확정된 주문은 수정할 수 없습니다")
        }
        
        val item = OrderItem(productId, productName, price, quantity)
        _items.add(item)
        recalculateTotalAmount()
        
        registerEvent(OrderItemAddedEvent(id, productId, quantity))
    }
    
    private fun recalculateTotalAmount() {
        totalAmount = _items.fold(Money.ZERO) { acc, item ->
            acc + (item.price * item.quantity)
        }
    }
}
```

### Repository 사용

```kotlin
@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {
    
    fun createUser(name: String, email: String): User {
        val userEmail = Email(email)
        
        if (userRepository.existsByEmail(userEmail)) {
            throw BusinessRuleException("이미 존재하는 이메일입니다")
        }
        
        val user = User(
            id = UserId.new(),
            name = name,
            email = userEmail
        )
        
        return userRepository.save(user)
    }
    
    fun updateUserProfile(userId: UserId, newName: String) {
        userRepository.update(userId) { user ->
            user.changeName(newName)
        }
    }
}

@Service
@Transactional  
class OrderService(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository
) {
    
    fun createOrder(userId: UserId, items: List<OrderItemRequest>): Order {
        val user = userRepository.findById(userId)
            ?: throw EntityNotFoundException("사용자를 찾을 수 없습니다")
        
        val order = Order(
            id = OrderId.new(),
            userId = user.id
        )
        
        items.forEach { item ->
            order.addItem(
                productId = item.productId,
                productName = item.productName,
                price = item.price,
                quantity = item.quantity
            )
        }
        
        return orderRepository.save(order)
    }
}
```

## 마이그레이션 가이드

### 기존 BinaryIdEntityRecord에서 마이그레이션

**Before (기존 코드):**
```kotlin
@Entity
class UserJpaRecord(
    id: BinaryId,
    var name: String,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?
) : BinaryIdEntityRecord<User>(id, createdAt, updatedAt, deletedAt) {
    // ...
}
```

**After (새로운 구조):**
```kotlin
@Entity
class UserJpaRecord(
    @Id
    @Convert(converter = UserIdConverter::class)
    var id: UserId,
    
    var name: String,
    
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?
) : EntityRecord<User, UserId>(createdAt, updatedAt, deletedAt) {
    // ...
}
```

### 주요 변경사항

1. **ID 필드 직접 정의**: `@Id`, `@Convert` 어노테이션을 직접 추가
2. **커스텀 컨버터**: 각 ID 타입에 맞는 JPA 컨버터 구현
3. **제네릭 타입**: `EntityRecord<E, ID>` 형태로 타입 안전성 확보
4. **유연한 ID 타입**: BinaryId뿐만 아니라 어떤 ID 타입도 사용 가능

이러한 구조로 Ball Framework의 JPA 모듈은 다양한 도메인 요구사항에 유연하게 대응할 수 있습니다.