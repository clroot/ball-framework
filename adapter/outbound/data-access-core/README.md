# Ball Framework Data Access Core

Ball Framework의 데이터 접근 어댑터들의 공통 기능을 제공하는 코어 모듈입니다. 도메인과 영속성 기술 간의 변환을 위한 추상화와 공통 예외 처리를 제공합니다.

## 목차
1. [모듈 개요](#모듈-개요)
2. [데이터 모델 매핑](#데이터-모델-매핑)
3. [예외 처리](#예외-처리)
4. [유틸리티 함수](#유틸리티-함수)
5. [사용 예시](#사용-예시)
6. [베스트 프랙티스](#베스트-프랙티스)

## 모듈 개요

Data Access Core 모듈은 다음과 같은 패키지 구조로 구성되어 있습니다:

```
adapter/outbound/data-access-core/
├── exception/                 # 데이터 접근 예외
│   ├── DatabaseException.kt
│   ├── DuplicateEntityException.kt
│   ├── EntityNotFoundException.kt
│   └── PersistenceException.kt
├── mapping/                   # 데이터 모델 매핑
│   ├── DataModel.kt
│   └── JsonAttributeHolder.kt
└── utils.kt                   # 유틸리티 함수
```

**주요 특징:**
- 도메인-영속성 변환 추상화
- 일관된 예외 처리
- JSON 속성 지원
- 영속성 기술 독립적 설계

## 데이터 모델 매핑

### DataModel 인터페이스

도메인 객체와 영속성 레코드 간의 변환을 담당하는 핵심 인터페이스입니다.

```kotlin
interface DataModel<E> {
    fun toDomain(): E
    fun update(entity: E)
}
```

**사용 원칙:**
- `toDomain()`: 영속성 레코드를 도메인 객체로 변환
- `update(entity)`: 도메인 객체의 변경사항을 영속성 레코드에 반영

### 구현 예시

```kotlin
// JPA 엔티티 레코드
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
    
    @Column(name = "age", nullable = false)
    var age: Int,
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: UserStatus,
    
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
) : EntityRecord<User, UserId>(createdAt, updatedAt, deletedAt) {

    // 기본 생성자 (JPA 요구사항)
    constructor() : this(
        id = UserId.new(),
        name = "",
        email = "",
        age = 0,
        status = UserStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        deletedAt = null
    )
    
    // 도메인 객체로부터 생성
    constructor(entity: User) : this(
        id = entity.id,
        name = entity.name,
        email = entity.email.value,
        age = entity.age,
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
            age = age,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }
    
    override fun update(entity: User) {
        this.name = entity.name
        this.email = entity.email.value
        this.age = entity.age
        this.status = entity.status
        updateCommonFields(entity)
    }
}
```

### JsonAttributeHolder

JSON 속성을 가진 엔티티를 위한 추상 클래스입니다.

```kotlin
abstract class JsonAttributeHolder {
    @Column(name = "attributes", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter::class)
    var attributes: Map<String, Any> = mutableMapOf()
        protected set
    
    protected fun <T> getAttribute(key: String, type: Class<T>): T? {
        return attributes[key]?.let { value ->
            when (type) {
                String::class.java -> value as? T
                Int::class.java -> (value as? Number)?.toInt() as? T
                Long::class.java -> (value as? Number)?.toLong() as? T
                Boolean::class.java -> value as? T
                else -> objectMapper.convertValue(value, type)
            }
        }
    }
    
    protected fun setAttribute(key: String, value: Any?) {
        val mutableAttributes = attributes.toMutableMap()
        if (value == null) {
            mutableAttributes.remove(key)
        } else {
            mutableAttributes[key] = value
        }
        attributes = mutableAttributes
    }
    
    protected fun hasAttribute(key: String): Boolean {
        return attributes.containsKey(key)
    }
}
```

**사용 예시:**
```kotlin
@Entity
@Table(name = "products")
class ProductJpaRecord(
    @Id
    var id: ProductId,
    
    @Column(name = "name", nullable = false)
    var name: String,
    
    @Column(name = "price", nullable = false)
    var price: BigDecimal,
    
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
) : EntityRecord<Product, ProductId>(createdAt, updatedAt, deletedAt),
    JsonAttributeHolder() {
    
    // 커스텀 속성들
    var tags: List<String>
        get() = getAttribute("tags", List::class.java) as? List<String> ?: emptyList()
        set(value) = setAttribute("tags", value)
    
    var metadata: Map<String, String>
        get() = getAttribute("metadata", Map::class.java) as? Map<String, String> ?: emptyMap()
        set(value) = setAttribute("metadata", value)
    
    override fun toDomain(): Product {
        return Product(
            id = id,
            name = name,
            price = Money(price),
            tags = tags,
            metadata = metadata,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }
    
    override fun update(entity: Product) {
        this.name = entity.name
        this.price = entity.price.amount
        this.tags = entity.tags
        this.metadata = entity.metadata
        updateCommonFields(entity)
    }
}
```

## 예외 처리

### 예외 계층 구조

```kotlin
// 기본 데이터베이스 예외
abstract class DatabaseException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// 엔티티 미발견
class EntityNotFoundException(
    entityType: String,
    entityId: String,
    cause: Throwable? = null
) : DatabaseException("$entityType not found: $entityId", cause) {
    
    companion object {
        fun <T : EntityBase<ID>, ID : Any> of(
            entityClass: KClass<T>,
            id: ID
        ): EntityNotFoundException {
            return EntityNotFoundException(
                entityType = entityClass.simpleName ?: "Entity",
                entityId = id.toString()
            )
        }
    }
}

// 중복 엔티티
class DuplicateEntityException(
    message: String,
    cause: Throwable? = null
) : DatabaseException(message, cause) {
    
    companion object {
        fun uniqueConstraintViolation(
            entityType: String,
            field: String,
            value: String
        ): DuplicateEntityException {
            return DuplicateEntityException(
                "$entityType with $field '$value' already exists"
            )
        }
    }
}

// 일반 영속성 예외
class PersistenceException(
    message: String,
    cause: Throwable? = null
) : DatabaseException(message, cause)
```

### 예외 변환 패턴

```kotlin
// Repository 어댑터에서 예외 변환
@Repository
class UserJpaAdapter(
    private val jpaRepository: UserJpaRepository
) : JpaRepositoryAdapter<User, UserId, UserJpaRecord>(jpaRepository), UserRepository {
    
    override fun save(entity: User): User {
        return try {
            super.save(entity)
        } catch (e: DataIntegrityViolationException) {
            // 제약 조건 위반을 도메인 예외로 변환
            when {
                e.message?.contains("email") == true -> {
                    throw DuplicateEntityException.uniqueConstraintViolation(
                        "User", "email", entity.email.value
                    )
                }
                else -> throw PersistenceException("사용자 저장 중 오류가 발생했습니다", e)
            }
        } catch (e: Exception) {
            throw PersistenceException("사용자 저장 중 알 수 없는 오류가 발생했습니다", e)
        }
    }
    
    override fun findById(id: UserId): User? {
        return try {
            jpaRepository.findById(id)?.toDomain()
        } catch (e: Exception) {
            throw PersistenceException("사용자 조회 중 오류가 발생했습니다", e)
        }
    }
    
    override fun findByEmail(email: Email): User? {
        return try {
            jpaRepository.findByEmail(email.value)?.toDomain()
        } catch (e: Exception) {
            throw PersistenceException("이메일로 사용자 조회 중 오류가 발생했습니다", e)
        }
    }
}
```

## 유틸리티 함수

### 변환 유틸리티

```kotlin
// 페이징 변환
fun <T, R> Page<T>.mapContent(transform: (T) -> R): Page<R> {
    return Page(
        content = content.map(transform),
        pageRequest = pageRequest,
        totalElements = totalElements
    )
}

// 옵셔널 변환
fun <T, R> T?.mapIfNotNull(transform: (T) -> R): R? {
    return this?.let(transform)
}

// 리스트 변환
fun <T, R> List<T>.mapToDomain(transform: (T) -> R): List<R> {
    return map(transform)
}

// 예외 안전 변환
inline fun <T, R> T.safeTransform(
    crossinline transform: (T) -> R,
    crossinline onError: (Exception) -> R
): R {
    return try {
        transform(this)
    } catch (e: Exception) {
        onError(e)
    }
}
```

### 검증 유틸리티

```kotlin
// ID 검증
fun <ID : Any> validateId(id: ID?): ID {
    return id ?: throw IllegalArgumentException("ID는 null일 수 없습니다")
}

// 엔티티 존재 검증
fun <T> requireEntity(entity: T?, entityType: String, id: String): T {
    return entity ?: throw EntityNotFoundException(entityType, id)
}

// 페이지 요청 검증
fun validatePageRequest(pageRequest: PageRequest): PageRequest {
    require(pageRequest.page >= 0) { "페이지 번호는 0 이상이어야 합니다" }
    require(pageRequest.size > 0) { "페이지 크기는 1 이상이어야 합니다" }
    require(pageRequest.size <= 1000) { "페이지 크기는 1000 이하여야 합니다" }
    return pageRequest
}
```

## 사용 예시

### 기본 Repository 어댑터 구현

```kotlin
@Repository
class OrderJpaAdapter(
    private val jpaRepository: OrderJpaRepository,
    private val orderItemRepository: OrderItemJpaRepository
) : JpaRepositoryAdapter<Order, OrderId, OrderJpaRecord>(jpaRepository), OrderRepository {
    
    override fun save(entity: Order): Order {
        return try {
            // 주문 저장
            val savedRecord = jpaRepository.save(OrderJpaRecord(entity))
            
            // 주문 항목들 저장
            entity.items.forEach { item ->
                orderItemRepository.save(OrderItemJpaRecord(item, savedRecord.id))
            }
            
            savedRecord.toDomain()
        } catch (e: DataIntegrityViolationException) {
            throw DuplicateEntityException("주문 저장 중 중복 오류가 발생했습니다", e)
        } catch (e: Exception) {
            throw PersistenceException("주문 저장 중 오류가 발생했습니다", e)
        }
    }
    
    override fun findByUserId(userId: UserId): List<Order> {
        return try {
            jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .mapToDomain { it.toDomain() }
        } catch (e: Exception) {
            throw PersistenceException("사용자 주문 조회 중 오류가 발생했습니다", e)
        }
    }
    
    override fun findByUserIdAndStatus(userId: UserId, status: OrderStatus): List<Order> {
        return safeTransform(
            transform = {
                jpaRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status)
                    .mapToDomain { it.toDomain() }
            },
            onError = { e ->
                throw PersistenceException("사용자 주문 상태별 조회 중 오류가 발생했습니다", e)
            }
        )
    }
}
```

### 복합 데이터 모델 구현

```kotlin
@Entity
@Table(name = "user_profiles")
class UserProfileJpaRecord(
    @Id
    @Convert(converter = UserIdConverter::class)
    var userId: UserId,
    
    @Column(name = "bio", length = 500)
    var bio: String?,
    
    @Column(name = "avatar_url")
    var avatarUrl: String?,
    
    @Column(name = "preferences", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter::class)
    var preferences: Map<String, Any> = mapOf(),
    
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
) : EntityRecord<UserProfile, UserId>(createdAt, updatedAt, deletedAt),
    JsonAttributeHolder() {
    
    // 선호도 관련 편의 프로퍼티들
    var theme: String
        get() = getAttribute("theme", String::class.java) ?: "light"
        set(value) = setAttribute("theme", value)
    
    var language: String
        get() = getAttribute("language", String::class.java) ?: "ko"
        set(value) = setAttribute("language", value)
    
    var notifications: Map<String, Boolean>
        get() = getAttribute("notifications", Map::class.java) as? Map<String, Boolean> ?: mapOf()
        set(value) = setAttribute("notifications", value)
    
    constructor() : this(
        userId = UserId.new(),
        bio = null,
        avatarUrl = null,
        preferences = mapOf(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        deletedAt = null
    )
    
    constructor(entity: UserProfile) : this(
        userId = entity.userId,
        bio = entity.bio,
        avatarUrl = entity.avatarUrl,
        preferences = mapOf(),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt
    ) {
        this.theme = entity.preferences.theme
        this.language = entity.preferences.language
        this.notifications = entity.preferences.notifications
    }
    
    override fun toDomain(): UserProfile {
        return UserProfile(
            userId = userId,
            bio = bio,
            avatarUrl = avatarUrl,
            preferences = UserPreferences(
                theme = theme,
                language = language,
                notifications = notifications
            ),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }
    
    override fun update(entity: UserProfile) {
        this.bio = entity.bio
        this.avatarUrl = entity.avatarUrl
        this.theme = entity.preferences.theme
        this.language = entity.preferences.language
        this.notifications = entity.preferences.notifications
        updateCommonFields(entity)
    }
}
```

## 베스트 프랙티스

### 1. DataModel 인터페이스 활용

```kotlin
// ✅ Good: DataModel 인터페이스 구현
@Entity
class UserJpaRecord(...) : EntityRecord<User, UserId>(...) {
    override fun toDomain(): User { /* 구현 */ }
    override fun update(entity: User) { /* 구현 */ }
}

// ❌ Bad: 수동 변환 메서드
@Entity
class UserJpaRecord(...) {
    fun toUser(): User { /* 구현 */ }
    fun updateFromUser(user: User) { /* 구현 */ }
}
```

### 2. 예외 변환

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

// ❌ Bad: 일반적인 예외 변환
override fun save(entity: User): User {
    return try {
        super.save(entity)
    } catch (e: Exception) {
        throw RuntimeException("저장 실패", e)
    }
}
```

### 3. JSON 속성 활용

```kotlin
// ✅ Good: JsonAttributeHolder 활용
class ProductJpaRecord(...) : JsonAttributeHolder() {
    var tags: List<String>
        get() = getAttribute("tags", List::class.java) as? List<String> ?: emptyList()
        set(value) = setAttribute("tags", value)
}

// ❌ Bad: 별도 JSON 직렬화
class ProductJpaRecord(...) {
    @Column(name = "tags_json")
    var tagsJson: String = ""
    
    var tags: List<String>
        get() = objectMapper.readValue(tagsJson, List::class.java) as List<String>
        set(value) { tagsJson = objectMapper.writeValueAsString(value) }
}
```

### 4. 유틸리티 함수 활용

```kotlin
// ✅ Good: 유틸리티 함수 사용
override fun findByUserId(userId: UserId): List<Order> {
    return safeTransform(
        transform = { 
            jpaRepository.findByUserId(userId).mapToDomain { it.toDomain() }
        },
        onError = { e -> 
            throw PersistenceException("주문 조회 실패", e)
        }
    )
}

// ❌ Bad: 반복적인 try-catch 블록
override fun findByUserId(userId: UserId): List<Order> {
    return try {
        val records = jpaRepository.findByUserId(userId)
        val orders = mutableListOf<Order>()
        for (record in records) {
            orders.add(record.toDomain())
        }
        orders
    } catch (e: Exception) {
        throw PersistenceException("주문 조회 실패", e)
    }
}
```

Ball Framework의 Data Access Core는 영속성 기술에 독립적인 추상화를 제공하여, 일관되고 안전한 데이터 접근 레이어를 구축할 수 있게 해줍니다.