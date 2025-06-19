# Ball Framework 기반 프로젝트 - Claude Memory

## 프로젝트 설정

- **프레임워크**: Ball Framework 2.0.0-20250618.10-SNAPSHOT+
- **언어**: Kotlin 2.1.20+, Java 21
- **주요 의존성**: Spring Boot 3.4.4+, Arrow 2.0.1+, Kotest 5.9.1+

## Ball Framework 아키텍처 준수사항

### 1. 헥사고날 아키텍처 레이어 구조

```
domain/          # 비즈니스 로직 (순수 Kotlin, Spring 의존성 없음)
├── model/       # 엔티티, 값 객체, 집합체
├── port/        # 인터페이스 정의
├── service/     # 도메인 서비스
└── exception/   # 도메인 예외

application/     # 유스케이스 (Spring 의존성 시작)
├── usecase/     # 커맨드 처리
├── query/       # 조회 처리
└── service/     # 애플리케이션 서비스

adapter/         # 외부 연동
├── inbound/     # REST API, 메시지 큐 등
└── outbound/    # 데이터베이스, 외부 API 등
```

### 2. 의존성 방향 규칙

- **절대 규칙**: Domain ← Application ← Adapter (역방향 금지)
- **Domain 순수성**: Spring, JPA 등 인프라 의존성 절대 금지
- **포트 기반 통신**: 외부 시스템은 반드시 포트 인터페이스 경유

### 3. 에러 처리 패턴

```kotlin
// Domain 레이어: 예외 기반
class User(...) : AggregateRoot<UserId>(...) {
    fun changeName(newName: String) {
        if (newName.isBlank()) {
            throw DomainValidationException("이름은 비어있을 수 없습니다")
        }
        this.name = newName
        registerEvent(UserNameChangedEvent(id, newName))
    }
}

// Application 레이어: Either 기반
class ChangeUserNameUseCase : UseCase<ChangeUserNameCommand, Unit>() {
    override suspend fun executeInternal(command: ChangeUserNameCommand) {
        // 도메인 예외는 자동으로 Either로 변환됨
        val user = userRepository.findById(command.userId)
            ?: throw EntityNotFoundException("사용자를 찾을 수 없습니다")
        
        user.changeName(command.newName)  // 도메인 예외 발생 가능
        userRepository.save(user)
    }
}

// Adapter 레이어: ResponseEntity 변환
@RestController
class UserController(private val changeUserName: ChangeUserNameUseCase) {
    @PutMapping("/users/{id}/name")
    fun changeName(@PathVariable id: String, @RequestBody request: ChangeNameRequest) =
        changeUserName.execute(ChangeUserNameCommand(UserId(id), request.name))
            .toResponseEntity()  // Either 확장 함수 사용
}
```

## 도메인 모델 작성 가이드

### 1. 엔티티 표준 구조

```kotlin
// 1. 커스텀 ID 타입 정의 (필수)
@JvmInline
value class UserId(val value: String) {
    companion object {
        fun new(): UserId = UserId(UUID.randomUUID().toString())
        fun from(value: String): UserId = UserId(value)
    }
}

// 2. 엔티티 정의
class User(
    id: UserId,                          // 식별자 (첫 번째)
    name: String,                        // 비즈니스 필드들
    email: Email,                        // 값 객체 활용
    status: UserStatus = UserStatus.ACTIVE,
    createdAt: LocalDateTime = LocalDateTime.now(),  // 메타데이터 (마지막)
    updatedAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null
) : AggregateRoot<UserId>(id, createdAt, updatedAt, deletedAt) {
    
    // 프로퍼티 캡슐화 (private setter)
    var name: String = name
        private set
    
    var email: Email = email
        private set
    
    var status: UserStatus = status
        private set
    
    // 비즈니스 메서드 (도메인 로직)
    fun changeName(newName: String) {
        validateName(newName)
        val oldName = this.name
        this.name = newName
        registerEvent(UserNameChangedEvent(id, oldName, newName))
    }
    
    fun changeEmail(newEmail: Email) {
        if (this.email == newEmail) return
        this.email = newEmail
        registerEvent(UserEmailChangedEvent(id, newEmail))
    }
    
    fun deactivate() {
        if (status == UserStatus.INACTIVE) {
            throw DomainStateException("이미 비활성화된 사용자입니다")
        }
        this.status = UserStatus.INACTIVE
        registerEvent(UserDeactivatedEvent(id))
    }
    
    // private 검증 메서드
    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw DomainValidationException("이름은 비어있을 수 없습니다")
        }
        if (name.length > 50) {
            throw DomainValidationException("이름은 50자를 초과할 수 없습니다")
        }
    }
}

// 3. 값 객체 정의
data class Email(val value: String) {
    init {
        require(value.matches(EMAIL_REGEX)) { 
            "올바른 이메일 형식이 아닙니다: $value" 
        }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
    }
}

// 4. 도메인 이벤트
data class UserNameChangedEvent(
    val userId: UserId,
    val oldName: String,
    val newName: String
) : DomainEvent
```

### 2. JPA 어댑터 구현

```kotlin
// JPA 엔티티
@Entity
@Table(name = "users")
class UserJpaRecord(
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false)
    var id: ByteArray,
    
    @Column(name = "name", nullable = false, length = 50)
    var name: String,
    
    @Column(name = "email", nullable = false, unique = true)
    var email: String,
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: UserStatus,
    
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?
) : EntityRecord<User>(createdAt, updatedAt, deletedAt) {
    
    constructor(user: User) : this(
        id = user.id.value.toBytes(),
        name = user.name,
        email = user.email.value,
        status = user.status,
        createdAt = user.createdAt,
        updatedAt = user.updatedAt,
        deletedAt = user.deletedAt
    )
    
    override fun update(entity: User) {
        this.name = entity.name
        this.email = entity.email.value
        this.status = entity.status
        updateCommonFields(entity)
    }
}
```

#### 3. Spring Data JPA Repository

```kotlin
interface UserJpaRepository : JpaRepository<UserJpaRecord, UserId> {
    fun findByEmailAndDeletedAtIsNull(email: String): UserJpaRecord?
    fun existsByEmailAndDeletedAtIsNull(email: String): Boolean
}
```

#### 4. Repository 어댑터 (JpaRepositoryAdapter 활용)

```kotlin
@Repository
class UserRepositoryAdapter(
    private val jpaRepository: UserJpaRepository
) : JpaRepositoryAdapter<User, UserId, UserJpaRecord>(jpaRepository), UserRepository {
    
    override fun createRecord(entity: User): UserJpaRecord = UserJpaRecord(entity)
    
    override fun findByEmail(email: Email): User? {
        return try {
            jpaRepository.findByEmailAndDeletedAtIsNull(email.value)?.toDomain()
        } catch (e: DataAccessException) {
            throw PersistenceException("이메일로 사용자 조회 중 오류 발생: ${email.value}", e)
        }
    }
    
    override fun existsByEmail(email: Email): Boolean {
        return try {
            jpaRepository.existsByEmailAndDeletedAtIsNull(email.value)
        } catch (e: DataAccessException) {
            throw PersistenceException("이메일 존재 여부 확인 중 오류 발생: ${email.value}", e)
        }
    }
}
```

#### 5. 집합체 루트 예시 (낙관적 잠금 지원)

```kotlin
@Entity
@Table(name = "orders")
class OrderJpaRecord(
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false)
    var id: ByteArray,
    
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    var userId: ByteArray,
    
    @Column(name = "total_amount", nullable = false)
    var totalAmount: BigDecimal,
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus,
    
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime?,
    version: Long = 0L
) : AggregateRootRecord<Order>(createdAt, updatedAt, deletedAt, version) {

    constructor(entity: Order, version: Long = 0L) : this(
        id = entity.id.value.toBytes(),
        userId = entity.userId.value.toBytes(),
        totalAmount = entity.totalAmount.amount,
        status = entity.status,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
        version = version
    )
    
    override fun update(entity: Order) {
        this.userId = entity.userId.value.toBytes()
        this.totalAmount = entity.totalAmount.amount
        this.status = entity.status
        updateCommonFields(entity)
    }
}
```

### 3. UseCase 구현

```kotlin
// 커맨드 정의
data class CreateUserCommand(
    val name: String,
    val email: String
)

// UseCase 구현
@Component
class CreateUserUseCase(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher
) : UseCase<CreateUserCommand, UserId>() {
    
    override suspend fun executeInternal(command: CreateUserCommand): UserId {
        // 중복 검사
        val email = Email(command.email)  // 검증은 값 객체에서
        
        if (userRepository.existsByEmail(email)) {
            throw BusinessRuleException("이미 사용 중인 이메일입니다")
        }
        
        // 엔티티 생성
        val user = User(
            id = UserId.new(),
            name = command.name,
            email = email
        )
        
        // 저장
        userRepository.save(user)
        
        // 이벤트 발행 (필요시)
        user.domainEvents().forEach { eventPublisher.publishEvent(it) }
        
        return user.id
    }
}

// 외부 API 연동이 필요한 경우
@Component
class SendNotificationUseCase(
    private val notificationService: NotificationService
) : UseCase<SendNotificationCommand, Unit>() {
    
    override suspend fun executeInternal(command: SendNotificationCommand) {
        try {
            notificationService.send(command.userId, command.message)
        } catch (e: Exception) {
            // 외부 시스템 오류는 ExternalSystemException으로 변환
            throw ExternalSystemException(
                "알림 전송 실패",
                systemName = "NotificationService",
                cause = e
            )
        }
    }
}
```

### 4. REST 컨트롤러

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(
    private val createUser: CreateUserUseCase,
    private val findUser: FindUserQuery
) {
    @PostMapping
    fun create(@RequestBody @Valid request: CreateUserRequest): ResponseEntity<CreateUserResponse> =
        createUser.execute(request.toCommand())
            .map { CreateUserResponse(it.value) }
            .toResponseEntity(HttpStatus.CREATED)
    
    @GetMapping("/{id}")
    fun findById(@PathVariable id: String): ResponseEntity<UserResponse> =
        findUser.execute(FindUserQuery.ById(UserId(id)))
            .map { UserResponse.from(it) }
            .toResponseEntity()
}
```

## 코드 작성 체크리스트

### 도메인 레이어

- [ ] 커스텀 ID 타입 정의 (UserId, OrderId 등)
- [ ] 엔티티 생성자 파라미터 순서: id → 비즈니스 필드 → 메타데이터
- [ ] 프로퍼티 캡슐화: `private set` 사용
- [ ] 비즈니스 로직을 엔티티 메서드로 구현
- [ ] 검증 실패시 도메인 예외 발생
- [ ] 중요한 상태 변경시 도메인 이벤트 등록
- [ ] 값 객체로 복잡한 검증 로직 분리
- [ ] 외부 시스템 오류는 ExternalSystemException으로 처리

### 애플리케이션 레이어

- [ ] UseCase 또는 Query 상속
- [ ] executeInternal 메서드 구현
- [ ] 도메인 예외는 자동으로 Either로 변환됨
- [ ] 트랜잭션은 UseCase가 자동 관리
- [ ] 필요시 ApplicationEventPublisher로 이벤트 발행

### 어댑터 레이어

- [ ] JPA: EntityRecord<E> 또는 AggregateRootRecord<E> 상속 (제네릭 사용)
- [ ] JPA: 커스텀 ID 타입 사용시 AttributeConverter 구현
- [ ] JPA: update() 메서드와 도메인 변환 생성자 구현
- [ ] JPA: JpaRepositoryAdapter 활용으로 반복 코드 제거
- [ ] JPA: Spring 예외는 Ball Framework 예외로 변환
- [ ] REST: Either 확장 함수로 ResponseEntity 변환
- [ ] REST: GlobalExceptionHandler가 예외 자동 처리

## 빌드 및 테스트

### 필수 명령어

```bash
./gradlew ktlintFormat   # 코드 포맷팅 (커밋 전 필수)
./gradlew test           # 테스트 실행
./gradlew build          # 전체 빌드
```

### 테스트 작성

- Kotest 사용 (JUnit 아님)
- 도메인 로직은 단위 테스트 필수
- 통합 테스트는 @SpringBootTest 활용

## 주의사항

### 하지 말아야 할 것들

1. **도메인에 Spring 의존성 추가** (절대 금지)
2. **서비스 레이어에 비즈니스 로직 작성** (도메인에 작성)
3. **빈혈 도메인 모델** (getter/setter만 있는 엔티티)
4. **Application/Adapter에서 직접 예외 던지기** (Either 사용)
5. **과도한 추상화** (YAGNI 원칙 준수)

### 반드시 해야 할 것들

1. **커스텀 ID 타입 사용** (String, Long 직접 사용 금지)
2. **값 객체 활용** (Email, Money, Address 등)
3. **도메인 이벤트 발행** (중요한 상태 변경시)
4. **Ball Framework 예외 사용** (도메인/애플리케이션 구분)
5. **코드 포맷팅** (ktlintFormat 실행)

이 가이드라인을 준수하여 Ball Framework의 설계 철학에 맞는 일관된 코드를 작성해주세요.