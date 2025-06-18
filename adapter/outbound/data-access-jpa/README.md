# Ball Framework JPA Data Access Module

Ball Framework의 JPA 데이터 접근 모듈로, Spring Data JPA를 기반으로 도메인 모델과 JPA 엔티티 간의 매핑을 지원합니다.

## 목차
1. [모듈 개요](#모듈-개요)
2. [기본 Record 클래스](#기본-record-클래스)
3. [JpaRepositoryAdapter](#jparepositryadapter)
4. [커스텀 ID 타입 사용법](#커스텀-id-타입-사용법)
5. [JPA 컨버터](#jpa-컨버터)
6. [자동 설정](#자동-설정)
7. [사용 예시](#사용-예시)
8. [베스트 프랙티스](#베스트-프랙티스)

## 모듈 개요

이 모듈은 도메인 모델과 JPA 엔티티 간의 매핑을 위한 기반 클래스들을 제공합니다. 
**ID 타입에 의존하지 않는 제네릭 구조**로 설계되어, 다양한 도메인 특화 ID 타입을 유연하게 지원합니다.

### 핵심 특징

- **제네릭 기반**: 어떤 ID 타입도 사용 가능 (BinaryId, UserId, OrderId 등)
- **타입 안전성**: 컴파일 타임에 타입 매칭 검증
- **JpaRepositoryAdapter**: 반복되는 변환 로직 제거
- **낙관적 잠금**: AggregateRoot용 버전 관리 내장
- **소프트 삭제**: deletedAt 필드 지원
- **자동 타임스탬프**: 생성/수정 시간 자동 관리
- **예외 변환**: Spring 예외를 Ball Framework 예외로 자동 변환

## 기본 Record 클래스

### EntityRecord<E, ID>

모든 JPA 엔티티 레코드의 기본 클래스입니다.

```kotlin
@MappedSuperclass
abstract class EntityRecord<E : EntityBase<ID>, ID : Any>(
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
) : DataModel<E> {
    
    @CreationTimestamp
    var createdAt: LocalDateTime = createdAt
        protected set

    @UpdateTimestamp
    var updatedAt: LocalDateTime = updatedAt
        protected set

    var deletedAt: LocalDateTime? = deletedAt
        protected set
        
    protected fun updateCommonFields(entity: E) {
        this.deletedAt = entity.deletedAt
        // 타임스탬프는 JPA가 자동 관리
    }
}
```

### AggregateRootRecord<E, ID>

집합체 루트를 위한 JPA 레코드 클래스로, 낙관적 잠금을 지원합니다.

```kotlin
@MappedSuperclass
abstract class AggregateRootRecord<E : AggregateRoot<ID>, ID : Any>(
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?,
    version: Long
) : EntityRecord<E, ID>(createdAt, updatedAt, deletedAt) {
    
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = version
        protected set
}
```

## JpaRepositoryAdapter

반복되는 변환 로직을 제거하고 일관된 예외 처리를 제공하는 제네릭 어댑터 클래스입니다.

### 기본 구조

```kotlin
abstract class JpaRepositoryAdapter<T : EntityBase<ID>, ID : Any, J : DataModel<T>>(
    protected val jpaRepository: JpaRepository<J, ID>
) : Repository<T, ID> {

    override fun findById(id: ID): T? {
        return try {
            jpaRepository.findById(id).orElse(null)?.toDomain()
        } catch (e: Exception) {
            throw PersistenceException("엔티티 조회 중 오류가 발생했습니다", e)
        }
    }

    override fun findAll(): List<T> {
        return try {
            jpaRepository.findAll().map { it.toDomain() }
        } catch (e: Exception) {
            throw PersistenceException("엔티티 목록 조회 중 오류가 발생했습니다", e)
        }
    }

    override fun save(entity: T): T {
        return try {
            val existingRecord = jpaRepository.findById(entity.id).orElse(null)
            val record = if (existingRecord != null) {
                existingRecord.update(entity)
                existingRecord
            } else {
                createRecord(entity)
            }
            jpaRepository.save(record).toDomain()
        } catch (e: DataIntegrityViolationException) {
            throw DuplicateEntityException("중복된 엔티티입니다", e)
        } catch (e: Exception) {
            throw PersistenceException("엔티티 저장 중 오류가 발생했습니다", e)
        }
    }

    override fun update(id: ID, modifier: (T) -> Unit): T {
        return try {
            val record = jpaRepository.findById(id).orElseThrow {
                EntityNotFoundException.of(entityClass, id)
            }
            val entity = record.toDomain()
            modifier(entity)
            record.update(entity)
            jpaRepository.save(record).toDomain()
        } catch (e: OptimisticLockingFailureException) {
            throw PersistenceException("동시 수정으로 인한 충돌이 발생했습니다", e)
        } catch (e: Exception) {
            throw PersistenceException("엔티티 수정 중 오류가 발생했습니다", e)
        }
    }

    override fun delete(entity: T) {
        delete(entity.id)
    }

    override fun delete(id: ID) {
        try {
            jpaRepository.deleteById(id)
        } catch (e: Exception) {
            throw PersistenceException("엔티티 삭제 중 오류가 발생했습니다", e)
        }
    }

    protected abstract fun createRecord(entity: T): J
}
```

**주요 특징:**
- **자동 변환**: 도메인 객체 ↔ JPA 레코드 자동 변환
- **예외 변환**: Spring 예외를 Ball Framework 예외로 변환
- **낙관적 잠금**: update 메서드에서 자동 처리
- **타입 안전성**: 제네릭으로 컴파일 타임 타입 체크

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

## JPA 컨버터

Ball Framework는 기본적으로 제공되는 JPA 컨버터들이 있습니다.

### 기본 제공 컨버터

```kotlin
// Duration 컨버터
@Converter(autoApply = true)
class DurationConverter : AttributeConverter<Duration, Long> {
    override fun convertToDatabaseColumn(attribute: Duration?): Long? {
        return attribute?.toMillis()
    }

    override fun convertToEntityAttribute(dbData: Long?): Duration? {
        return dbData?.let { Duration.ofMillis(it) }
    }
}

// LocalDateTime 컨버터
@Converter(autoApply = true)
class LocalDateTimeConverter : AttributeConverter<LocalDateTime, Timestamp> {
    override fun convertToDatabaseColumn(attribute: LocalDateTime?): Timestamp? = attribute?.let { Timestamp.valueOf(it) }

    override fun convertToEntityAttribute(dbData: Timestamp?): LocalDateTime? {
        return dbData?.toLocalDateTime()?.atZone(ZoneOffset.UTC)?.toLocalDateTime()
    }
}
```

### 커스텀 ID 컨버터 정의

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
    
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
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
    
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?,
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

## 자동 설정

### BallPersistenceJpaAutoConfiguration

```kotlin
@AutoConfiguration
@ConditionalOnClass(JpaRepository::class)
@EnableJpaRepositories
@EnableJpaAuditing
class BallPersistenceJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun binaryIdConverter(): BinaryIdConverter {
        return BinaryIdConverter()
    }

    @Bean
    @ConditionalOnMissingBean
    fun durationConverter(): DurationConverter {
        return DurationConverter()
    }

    @Bean
    @ConditionalOnMissingBean
    fun localDateTimeConverter(): LocalDateTimeConverter {
        return LocalDateTimeConverter()
    }
}
```

### JPA 감사 설정

```kotlin
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig {
    // JPA Auditing 자동 설정
}
```

**사용 시 설정:**
```yaml
# application.yml
ball:
  adapter:
    jpa:
      enabled: true
      auditing:
        enabled: true

spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
    show-sql: false
  datasource:
    url: jdbc:mysql://localhost:3306/ball_framework
    username: ball_user
    password: ball_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## JPA 어댑터 구현

### JpaRepositoryAdapter 기본 클래스 활용

```kotlin
@Repository
class UserJpaAdapter(
    private val jpaRepository: UserJpaRepository
) : JpaRepositoryAdapter<User, UserId, UserJpaRecord>(jpaRepository), UserRepository {
    
    override fun createRecord(entity: User): UserJpaRecord {
        return UserJpaRecord(entity)
    }
    
    override fun findByEmail(email: Email): User? {
        return try {
            jpaRepository.findByEmail(email.value)?.toDomain()
        } catch (e: Exception) {
            throw PersistenceException("이메일로 사용자 조회 중 오류가 발생했습니다", e)
        }
    }
    
    override fun existsByEmail(email: Email): Boolean {
        return try {
            jpaRepository.existsByEmail(email.value)
        } catch (e: Exception) {
            throw PersistenceException("이메일 중복 확인 중 오류가 발생했습니다", e)
        }
    }
    
    override fun findAllActive(): List<User> {
        return try {
            jpaRepository.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE)
                .map { it.toDomain() }
        } catch (e: Exception) {
            throw PersistenceException("활성 사용자 목록 조회 중 오류가 발생했습니다", e)
        }
    }
}

@Repository
class OrderJpaAdapter(
    private val jpaRepository: OrderJpaRepository
) : JpaRepositoryAdapter<Order, OrderId, OrderJpaRecord>(jpaRepository), OrderRepository {
    
    override fun createRecord(entity: Order): OrderJpaRecord {
        return OrderJpaRecord(entity)
    }
    
    override fun findByUserId(userId: UserId): List<Order> {
        return try {
            jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .map { it.toDomain() }
        } catch (e: Exception) {
            throw PersistenceException("사용자 주문 목록 조회 중 오류가 발생했습니다", e)
        }
    }
    
    override fun findByUserIdAndStatus(userId: UserId, status: OrderStatus): List<Order> {
        return try {
            jpaRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status)
                .map { it.toDomain() }
        } catch (e: Exception) {
            throw PersistenceException("사용자 주문 상태별 조회 중 오류가 발생했습니다", e)
        }
    }
    
    override fun countByUserIdAndStatus(userId: UserId, status: OrderStatus): Int {
        return try {
            jpaRepository.countByUserIdAndStatus(userId, status)
        } catch (e: Exception) {
            throw PersistenceException("사용자 주문 개수 조회 중 오류가 발생했습니다", e)
        }
    }
}
```

### Spring Data JPA Repository

```kotlin
interface UserJpaRepository : JpaRepository<UserJpaRecord, UserId> {
    fun findByEmail(email: String): UserJpaRecord?
    fun existsByEmail(email: String): Boolean
    fun findByStatusAndDeletedAtIsNull(status: UserStatus): List<UserJpaRecord>
    fun findByNameContainingIgnoreCaseAndDeletedAtIsNull(name: String): List<UserJpaRecord>
}

interface OrderJpaRepository : JpaRepository<OrderJpaRecord, OrderId> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UserId): List<OrderJpaRecord>
    fun findByUserIdAndStatusOrderByCreatedAtDesc(userId: UserId, status: OrderStatus): List<OrderJpaRecord>
    fun countByUserIdAndStatus(userId: UserId, status: OrderStatus): Int
    fun findByStatusAndCreatedAtBetween(
        status: OrderStatus, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): List<OrderJpaRecord>
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
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null
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
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null
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

### UseCase에서 Repository 사용

```kotlin
@Service
class CreateUserUseCase(
    applicationEventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository,
    private val userPolicy: UserPolicy
) : UseCase<CreateUserCommand, User>(applicationEventPublisher) {
    
    override fun executeInternal(command: CreateUserCommand): User {
        val userEmail = Email(command.email)
        
        if (userRepository.existsByEmail(userEmail)) {
            throw BusinessRuleException.policyViolation(
                "EMAIL_DUPLICATE",
                "이미 존재하는 이메일입니다"
            )
        }
        
        val user = User(
            id = UserId.new(),
            name = command.name,
            email = userEmail,
            age = command.age
        )
        
        userPolicy.validate(user)
        
        return userRepository.save(user)
    }
}

@Service  
class UpdateUserProfileUseCase(
    applicationEventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository
) : UseCase<UpdateUserProfileCommand, User>(applicationEventPublisher) {
    
    override fun executeInternal(command: UpdateUserProfileCommand): User {
        return userRepository.update(command.userId) { user ->
            user.changeName(command.name)
            user.changeEmail(Email(command.email))
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

## 베스트 프랙티스

### 1. JpaRepositoryAdapter 활용

```kotlin
// ✅ Good: JpaRepositoryAdapter 상속으로 반복 코드 제거
@Repository
class UserJpaAdapter(
    private val jpaRepository: UserJpaRepository
) : JpaRepositoryAdapter<User, UserId, UserJpaRecord>(jpaRepository), UserRepository {
    
    override fun createRecord(entity: User): UserJpaRecord = UserJpaRecord(entity)
    
    override fun findByEmail(email: Email): User? {
        return safeExecute { jpaRepository.findByEmail(email.value)?.toDomain() }
    }
}

// ❌ Bad: 수동으로 모든 메서드 구현
@Repository
class UserJpaAdapter(
    private val jpaRepository: UserJpaRepository
) : UserRepository {
    
    override fun findById(id: UserId): User? {
        return try {
            jpaRepository.findById(id).orElse(null)?.toDomain()
        } catch (e: Exception) {
            throw PersistenceException("사용자 조회 실패", e)
        }
    }
    
    // 모든 기본 메서드를 수동으로 구현해야 함...
}
```

### 2. 예외 처리

```kotlin
// ✅ Good: 구체적인 예외 변환
override fun save(entity: User): User {
    return try {
        super.save(entity)
    } catch (e: DataIntegrityViolationException) {
        when {
            e.message?.contains("email") == true -> 
                throw DuplicateEntityException.uniqueConstraintViolation("User", "email", entity.email.value)
            else -> throw PersistenceException("사용자 저장 실패", e)
        }
    }
}

// ❌ Bad: 일반적인 예외 처리
override fun save(entity: User): User {
    return try {
        super.save(entity)
    } catch (e: Exception) {
        throw RuntimeException("저장 실패", e)
    }
}
```

### 3. Repository update 메서드 활용

```kotlin
// ✅ Good: update 메서드로 낙관적 잠금 활용
fun updateUserProfile(userId: UserId, newName: String) {
    userRepository.update(userId) { user ->
        user.changeName(newName)
    }
}

// ❌ Bad: 조회 후 저장으로 경쟁 상태 위험
fun updateUserProfile(userId: UserId, newName: String) {
    val user = userRepository.findById(userId)!!
    user.changeName(newName)
    userRepository.save(user)  // 동시성 문제 발생 가능
}
```

### 5. 소프트 삭제 고려

```kotlin
// ✅ Good: 소프트 삭제를 고려한 쿼리
interface UserJpaRepository : JpaRepository<UserJpaRecord, UserId> {
    fun findByStatusAndDeletedAtIsNull(status: UserStatus): List<UserJpaRecord>
    fun findByNameContainingIgnoreCaseAndDeletedAtIsNull(name: String): List<UserJpaRecord>
}

// ❌ Bad: 삭제된 데이터 포함
interface UserJpaRepository : JpaRepository<UserJpaRecord, UserId> {
    fun findByStatus(status: UserStatus): List<UserJpaRecord>  // 삭제된 데이터도 조회됨
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
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
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
    
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
) : EntityRecord<User, UserId>(createdAt, updatedAt, deletedAt) {
    // ...
}
```

### 주요 변경사항

1. **ID 필드 직접 정의**: `@Id`, `@Convert` 어노테이션을 직접 추가
2. **커스텀 컨버터**: 각 ID 타입에 맞는 JPA 컨버터 구현
3. **제네릭 타입**: `EntityRecord<E, ID>` 형태로 타입 안전성 확보
4. **유연한 ID 타입**: BinaryId뿐만 아니라 어떤 ID 타입도 사용 가능

### Kotlin JDSL 지원

Ball Framework의 JPA 모듈은 복잡한 쿼리를 위해 Kotlin JDSL을 지원합니다:

```kotlin
// 복잡한 동적 쿼리 예시
@Repository
class OrderQueryRepository(
    private val entityManager: EntityManager
) {
    
    fun findOrdersWithCriteria(criteria: OrderSearchCriteria): List<Order> {
        return jpql {
            select(
                entity(OrderJpaRecord::class)
            ).from(
                entity(OrderJpaRecord::class)
            ).whereAnd(
                entity(OrderJpaRecord::class).get(OrderJpaRecord::userId).equal(criteria.userId),
                entity(OrderJpaRecord::class).get(OrderJpaRecord::status).`in`(criteria.statuses),
                entity(OrderJpaRecord::class).get(OrderJpaRecord::createdAt)
                    .between(criteria.startDate, criteria.endDate)
            ).orderBy(
                entity(OrderJpaRecord::class).get(OrderJpaRecord::createdAt).desc()
            )
        }.executeQuery(entityManager)
         .map { it.toDomain() }
    }
}
```

Ball Framework의 JPA 모듈은 다양한 도메인 요구사항에 유연하게 대응할 수 있으며, 타입 안전성과 개발자 경험을 모두 만족시키는 데이터 접근 레이어를 제공합니다.