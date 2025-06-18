# Ball Framework Application Module

Ball Framework의 애플리케이션 레이어 모듈로, CQRS(Command Query Responsibility Segregation) 패턴과 함수형 에러 처리를 기반으로 하는 유스케이스를 제공합니다.

## 목차
1. [모듈 개요](#모듈-개요)
2. [UseCase 패턴](#usecase-패턴)
3. [Query 패턴](#query-패턴)
4. [애플리케이션 에러 처리](#애플리케이션-에러-처리)
5. [도메인 확장 함수](#도메인-확장-함수)
6. [실제 사용 예시](#실제-사용-예시)
7. [베스트 프랙티스](#베스트-프랙티스)

## 모듈 개요

Ball Framework의 Application 모듈은 다음과 같은 패키지 구조로 구성되어 있습니다:

```
application/
├── usecase/         # UseCase와 Query 추상 클래스
├── extensions/      # 도메인 확장 함수
└── ApplicationError.kt  # 애플리케이션 에러 정의
```

**주요 의존성:**
- Domain 모듈 (api)
- Spring Boot Starter (트랜잭션 지원)
- Spring TX (트랜잭션 관리)
- Shared Arrow (함수형 에러 처리)
- Shared Lock (분산 락 지원)

## UseCase 패턴

### 기본 UseCase 추상 클래스

```kotlin
abstract class UseCase<TCommand, TResult>(
    protected val applicationEventPublisher: ApplicationEventPublisher
) {
    @Transactional
    open fun execute(command: TCommand): Either<ApplicationError, TResult> {
        return try {
            val result = executeInternal(command)
            
            // 도메인 이벤트 자동 발행
            if (result is AggregateRoot<*>) {
                result.domainEvents.forEach { event ->
                    applicationEventPublisher.publishEvent(event)
                }
                result.clearEvents()
            }
            
            Either.Right(result)
        } catch (exception: DomainException) {
            Either.Left(ApplicationError.DomainError(exception))
        } catch (exception: Exception) {
            Either.Left(ApplicationError.SystemError(exception))
        }
    }
    
    protected abstract fun executeInternal(command: TCommand): TResult
}
```

**주요 특징:**
- **트랜잭션 자동 관리**: `@Transactional` 어노테이션으로 트랜잭션 범위 설정
- **도메인 이벤트 자동 발행**: `AggregateRoot`의 도메인 이벤트를 자동으로 발행
- **예외 변환**: 도메인 예외를 `ApplicationError`로 자동 변환
- **함수형 에러 처리**: `Either<ApplicationError, TResult>` 반환

### UseCase 구현 예시

```kotlin
// 명령 객체 정의
data class CreateUserCommand(
    val name: String,
    val email: String,
    val age: Int
)

// UseCase 구현
@Service
class CreateUserUseCase(
    applicationEventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository,
    private val userPolicy: UserPolicy
) : UseCase<CreateUserCommand, User>(applicationEventPublisher) {

    override fun executeInternal(command: CreateUserCommand): User {
        // 1. 이메일 중복 검증
        val email = Email(command.email)
        if (userRepository.existsByEmail(email)) {
            throw BusinessRuleException.policyViolation(
                "EMAIL_DUPLICATE", 
                "이미 존재하는 이메일입니다"
            )
        }
        
        // 2. 사용자 생성
        val user = User(
            id = BinaryId.new(),
            name = command.name,
            email = email,
            age = command.age
        )
        
        // 3. 정책 검증
        userPolicy.validate(user)
        
        // 4. 저장 (도메인 이벤트는 자동 발행됨)
        return userRepository.save(user)
    }
}
```

### UseCase 호출 및 에러 처리

```kotlin
@RestController
class UserController(
    private val createUserUseCase: CreateUserUseCase
) {
    
    @PostMapping("/users")
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val command = CreateUserCommand(
            name = request.name,
            email = request.email,
            age = request.age
        )
        
        return createUserUseCase.execute(command).fold(
            ifLeft = { error -> 
                when (error) {
                    is ApplicationError.DomainError -> {
                        when (error.exception) {
                            is DomainValidationException -> ResponseEntity.badRequest().build()
                            is BusinessRuleException -> ResponseEntity.status(409).build()
                            else -> ResponseEntity.status(500).build()
                        }
                    }
                    is ApplicationError.SystemError -> ResponseEntity.status(500).build()
                }
            },
            ifRight = { user -> 
                ResponseEntity.ok(UserResponse.from(user))
            }
        )
    }
}
```

## Query 패턴

### 기본 Query 추상 클래스

```kotlin
abstract class Query<TQuery, TResult> {
    @Transactional(readOnly = true)
    open fun execute(query: TQuery): Either<ApplicationError, TResult> {
        return try {
            val result = executeInternal(query)
            Either.Right(result)
        } catch (exception: DomainException) {
            Either.Left(ApplicationError.DomainError(exception))
        } catch (exception: Exception) {
            Either.Left(ApplicationError.SystemError(exception))
        }
    }
    
    protected abstract fun executeInternal(query: TQuery): TResult
}
```

**주요 특징:**
- **읽기 전용 트랜잭션**: `@Transactional(readOnly = true)`로 성능 최적화
- **CQRS 분리**: 명령(Command)과 조회(Query) 분리
- **동일한 에러 처리**: UseCase와 동일한 에러 처리 메커니즘

### Query 구현 예시

```kotlin
// 쿼리 객체 정의
data class GetUserByEmailQuery(
    val email: String
)

// Query 구현
@Service
class GetUserByEmailQuery(
    private val userRepository: UserRepository
) : Query<GetUserByEmailQuery, User?>() {

    override fun executeInternal(query: GetUserByEmailQuery): User? {
        val email = Email(query.email)
        return userRepository.findByEmail(email)
    }
}

// 페이징 쿼리 예시
data class GetUsersQuery(
    val pageRequest: PageRequest,
    val namePattern: String? = null,
    val status: UserStatus? = null
)

@Service
class GetUsersQuery(
    private val userRepository: UserRepository
) : Query<GetUsersQuery, Page<User>>() {

    override fun executeInternal(query: GetUsersQuery): Page<User> {
        val specification = buildSpecification(query)
        return userRepository.findBySpecification(specification, query.pageRequest)
    }
    
    private fun buildSpecification(query: GetUsersQuery): Specification<User> {
        var spec = Specification.all<User>()
        
        query.namePattern?.let { pattern ->
            spec = spec.and(UserSpecifications.nameContains(pattern))
        }
        
        query.status?.let { status ->
            spec = spec.and(UserSpecifications.hasStatus(status))
        }
        
        return spec
    }
}
```

## 애플리케이션 에러 처리

### ApplicationError 계층

```kotlin
sealed class ApplicationError {
    abstract val message: String
    abstract val timestamp: LocalDateTime
    
    data class DomainError(
        val exception: DomainException,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : ApplicationError() {
        override val message: String = exception.message ?: "도메인 에러가 발생했습니다"
        
        val errorCode: String = when (exception) {
            is DomainValidationException -> "VALIDATION_ERROR"
            is BusinessRuleException -> "BUSINESS_RULE_ERROR"
            is DomainStateException -> "DOMAIN_STATE_ERROR"
            is ExternalSystemException -> "EXTERNAL_SYSTEM_ERROR"
            else -> "DOMAIN_ERROR"
        }
    }
    
    data class SystemError(
        val exception: Exception,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : ApplicationError() {
        override val message: String = "시스템 에러가 발생했습니다"
        val errorCode: String = "SYSTEM_ERROR"
    }
}
```

### 에러 처리 패턴

```kotlin
// UseCase 결과 처리
fun handleUserCreation(command: CreateUserCommand): UserResponse {
    return createUserUseCase.execute(command).fold(
        ifLeft = { error ->
            when (error) {
                is ApplicationError.DomainError -> {
                    val domainError = error.exception
                    when (domainError) {
                        is DomainValidationException -> {
                            throw ValidationException(
                                field = domainError.field,
                                message = domainError.message,
                                code = domainError.code
                            )
                        }
                        is BusinessRuleException -> {
                            throw BusinessException(
                                code = domainError.ruleCode,
                                message = domainError.message
                            )
                        }
                        else -> throw SystemException(domainError.message)
                    }
                }
                is ApplicationError.SystemError -> {
                    throw SystemException("시스템 오류가 발생했습니다")
                }
            }
        },
        ifRight = { user -> UserResponse.from(user) }
    )
}

// Either 확장 함수 활용
fun createUserAndReturnResponse(command: CreateUserCommand): Either<String, UserResponse> {
    return createUserUseCase.execute(command)
        .mapLeft { error -> error.message }
        .map { user -> UserResponse.from(user) }
}
```

## 도메인 확장 함수

### EntityBase 확장 함수

```kotlin
// EntityBase 확장 함수들
fun <T : EntityBase<ID>, ID : Any> T.isDeleted(): Boolean = deletedAt != null

fun <T : EntityBase<ID>, ID : Any> T.delete(): T {
    return this.copy(deletedAt = LocalDateTime.now()) as T
}

fun <T : EntityBase<ID>, ID : Any> T.restore(): T {
    return this.copy(deletedAt = null) as T
}

fun <T : EntityBase<ID>, ID : Any> T.updateTimestamp(): T {
    return this.copy(updatedAt = LocalDateTime.now()) as T
}
```

**사용 예시:**
```kotlin
// UseCase에서 확장 함수 활용
@Service
class DeleteUserUseCase(
    applicationEventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository
) : UseCase<DeleteUserCommand, User>(applicationEventPublisher) {

    override fun executeInternal(command: DeleteUserCommand): User {
        val user = userRepository.findById(command.userId)
            ?: throw DomainStateException.entityNotFound(User::class, command.userId.toString())
        
        if (user.isDeleted()) {
            throw DomainStateException.invalidState(
                User::class,
                "이미 삭제된 사용자입니다"
            )
        }
        
        val deletedUser = user.delete()
        return userRepository.save(deletedUser)
    }
}
```

## 실제 사용 예시

### 복합 UseCase 예시

```kotlin
// 주문 생성 명령
data class CreateOrderCommand(
    val userId: BinaryId,
    val items: List<OrderItemData>
)

data class OrderItemData(
    val productId: BinaryId,
    val productName: String,
    val price: Money,
    val quantity: Int
)

// 복합 UseCase 구현
@Service
class CreateOrderUseCase(
    applicationEventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val orderPolicy: OrderPolicy,
    private val inventoryService: InventoryService
) : UseCase<CreateOrderCommand, Order>(applicationEventPublisher) {

    override fun executeInternal(command: CreateOrderCommand): Order {
        // 1. 사용자 검증
        val user = userRepository.findById(command.userId)
            ?: throw DomainStateException.entityNotFound(User::class, command.userId.toString())
        
        // 2. 정책 검증
        orderPolicy.validate(user)
        
        // 3. 상품 검증 및 재고 확인
        val validatedItems = validateAndReserveItems(command.items)
        
        // 4. 주문 생성
        val order = Order(
            id = BinaryId.new(),
            userId = user.id
        )
        
        // 5. 주문 항목 추가
        validatedItems.forEach { item ->
            order.addItem(
                productId = item.productId,
                productName = item.productName,
                price = item.price,
                quantity = item.quantity
            )
        }
        
        // 6. 주문 확정
        order.confirm()
        
        // 7. 저장 (도메인 이벤트 자동 발행)
        return orderRepository.save(order)
    }
    
    private fun validateAndReserveItems(items: List<OrderItemData>): List<OrderItemData> {
        return items.map { item ->
            // 상품 존재 확인
            val product = productRepository.findById(item.productId)
                ?: throw DomainStateException.entityNotFound(Product::class, item.productId.toString())
            
            // 재고 확인 및 예약
            if (!inventoryService.canReserve(item.productId, item.quantity)) {
                throw BusinessRuleException.policyViolation(
                    "INSUFFICIENT_INVENTORY",
                    "재고가 부족합니다: ${product.name}"
                )
            }
            
            inventoryService.reserve(item.productId, item.quantity)
            
            item.copy(
                productName = product.name,
                price = product.price
            )
        }
    }
}
```

### 이벤트 처리와 보상 트랜잭션

```kotlin
// 주문 이벤트 핸들러
@Component
class OrderEventHandler(
    private val inventoryService: InventoryService,
    private val paymentService: PaymentService,
    private val notificationService: NotificationService
) {
    
    @EventListener
    @Async
    fun handleOrderConfirmed(event: OrderConfirmedEvent) {
        try {
            // 결제 처리
            paymentService.processPayment(event.orderId, event.totalAmount)
            
            // 재고 확정
            inventoryService.confirmReservation(event.orderId)
            
            // 알림 발송
            notificationService.sendOrderConfirmation(event.userId, event.orderId)
            
        } catch (exception: Exception) {
            // 보상 트랜잭션
            compensateOrderConfirmation(event, exception)
        }
    }
    
    private fun compensateOrderConfirmation(event: OrderConfirmedEvent, exception: Exception) {
        try {
            // 재고 예약 취소
            inventoryService.cancelReservation(event.orderId)
            
            // 주문 취소 이벤트 발행
            applicationEventPublisher.publishEvent(
                OrderCancelledEvent(
                    orderId = event.orderId,
                    reason = "시스템 오류로 인한 자동 취소",
                    causedBy = exception
                )
            )
        } catch (compensationException: Exception) {
            // 보상 실패 시 수동 처리를 위한 알림
            notificationService.sendCompensationFailedAlert(event.orderId, compensationException)
        }
    }
}
```

### 분산 락을 활용한 UseCase

```kotlin
@Service
class TransferMoneyUseCase(
    applicationEventPublisher: ApplicationEventPublisher,
    private val accountRepository: AccountRepository
) : UseCase<TransferMoneyCommand, TransferResult>(applicationEventPublisher) {

    @DistributedLock(
        key = "transfer",
        timeout = 5000,
        leaseTime = 10000
    )
    override fun executeInternal(
        @LockKey("fromAccountId") command: TransferMoneyCommand
    ): TransferResult {
        // 분산 락으로 계좌 간 이체 동시성 제어
        
        val fromAccount = accountRepository.findById(command.fromAccountId)
            ?: throw DomainStateException.entityNotFound(Account::class, command.fromAccountId.toString())
        
        val toAccount = accountRepository.findById(command.toAccountId)
            ?: throw DomainStateException.entityNotFound(Account::class, command.toAccountId.toString())
        
        // 출금
        fromAccount.withdraw(command.amount)
        
        // 입금
        toAccount.deposit(command.amount)
        
        // 저장
        accountRepository.save(fromAccount)
        accountRepository.save(toAccount)
        
        return TransferResult(
            fromAccountId = command.fromAccountId,
            toAccountId = command.toAccountId,
            amount = command.amount,
            completedAt = LocalDateTime.now()
        )
    }
}
```

## 베스트 프랙티스

### 1. 명령과 조회 분리 (CQRS)

```kotlin
// ✅ Good: 명령과 조회 분리
@Service
class CreateUserUseCase(...) : UseCase<CreateUserCommand, User>(...) {
    // 상태 변경 로직
}

@Service
class GetUserQuery(...) : Query<GetUserQuery, User?>() {
    // 조회 로직
}

// ❌ Bad: 명령과 조회 혼재
@Service
class UserService {
    fun createUser(...): User { ... }
    fun getUser(...): User? { ... }
    fun updateUser(...): User { ... }
    fun deleteUser(...): Unit { ... }
}
```

### 2. 도메인 이벤트 자동 발행

```kotlin
// ✅ Good: UseCase에서 도메인 이벤트 자동 발행
@Service
class ConfirmOrderUseCase(...) : UseCase<ConfirmOrderCommand, Order>(...) {
    override fun executeInternal(command: ConfirmOrderCommand): Order {
        val order = orderRepository.findById(command.orderId)!!
        order.confirm() // 도메인 이벤트 등록
        return orderRepository.save(order) // UseCase에서 자동 발행
    }
}

// ❌ Bad: 수동 이벤트 발행
@Service
class ConfirmOrderUseCase(...) {
    fun execute(command: ConfirmOrderCommand): Order {
        val order = orderRepository.findById(command.orderId)!!
        order.confirm()
        orderRepository.save(order)
        
        // 수동으로 이벤트 발행 (누락 가능성)
        applicationEventPublisher.publishEvent(OrderConfirmedEvent(...))
        return order
    }
}
```

### 3. 함수형 에러 처리

```kotlin
// ✅ Good: Either를 활용한 함수형 에러 처리
fun createUserAndNotify(command: CreateUserCommand): Either<String, UserResponse> {
    return createUserUseCase.execute(command)
        .flatMap { user ->
            notificationService.sendWelcomeEmail(user.email)
                .map { UserResponse.from(user) }
        }
        .mapLeft { error -> error.message }
}

// ❌ Bad: 예외 기반 에러 처리
fun createUserAndNotify(command: CreateUserCommand): UserResponse {
    try {
        val result = createUserUseCase.execute(command)
        when (result) {
            is Either.Right -> {
                notificationService.sendWelcomeEmail(result.value.email)
                return UserResponse.from(result.value)
            }
            is Either.Left -> throw Exception(result.value.message)
        }
    } catch (e: Exception) {
        throw RuntimeException("사용자 생성 실패", e)
    }
}
```

### 4. 트랜잭션 범위 관리

```kotlin
// ✅ Good: UseCase 단위로 트랜잭션 관리
@Service
class ProcessOrderUseCase(...) : UseCase<ProcessOrderCommand, OrderResult>(...) {
    // @Transactional이 UseCase 레벨에 자동 적용
    override fun executeInternal(command: ProcessOrderCommand): OrderResult {
        // 모든 작업이 하나의 트랜잭션에서 실행
        val order = createOrder(command)
        processPayment(order)
        updateInventory(order)
        return OrderResult.from(order)
    }
}

// ❌ Bad: 세분화된 트랜잭션
@Service
class OrderService {
    @Transactional
    fun createOrder(...): Order { ... }
    
    @Transactional
    fun processPayment(...): Payment { ... }
    
    @Transactional
    fun updateInventory(...): Unit { ... }
    
    // 트랜잭션 범위가 분산되어 일관성 문제 발생 가능
    fun processOrder(command: ProcessOrderCommand): OrderResult {
        val order = createOrder(command)
        processPayment(order)
        updateInventory(order)
        return OrderResult.from(order)
    }
}
```

### 5. 명령 객체 검증

```kotlin
// ✅ Good: 명령 객체에서 기본 검증
data class CreateUserCommand(
    val name: String,
    val email: String,
    val age: Int
) {
    init {
        require(name.isNotBlank()) { "이름은 필수입니다" }
        require(email.isNotBlank()) { "이메일은 필수입니다" }
        require(age >= 0) { "나이는 0 이상이어야 합니다" }
    }
}

// UseCase에서는 도메인 로직에 집중
@Service
class CreateUserUseCase(...) : UseCase<CreateUserCommand, User>(...) {
    override fun executeInternal(command: CreateUserCommand): User {
        // 기본 검증은 이미 완료, 도메인 로직에만 집중
        val email = Email(command.email) // 도메인 검증
        // ...
    }
}

// ❌ Bad: UseCase에서 기본 검증
@Service
class CreateUserUseCase(...) : UseCase<CreateUserCommand, User>(...) {
    override fun executeInternal(command: CreateUserCommand): User {
        // 기본 검증과 도메인 로직이 혼재
        if (command.name.isBlank()) throw IllegalArgumentException("이름은 필수입니다")
        if (command.email.isBlank()) throw IllegalArgumentException("이메일은 필수입니다")
        
        val email = Email(command.email)
        // ...
    }
}
```

## 마무리

Ball Framework의 Application 모듈은 다음과 같은 이점을 제공합니다:

1. **CQRS 패턴**: 명령과 조회의 명확한 분리로 복잡성 감소
2. **함수형 에러 처리**: `Either` 타입으로 안전한 에러 처리
3. **자동 트랜잭션 관리**: 선언적 트랜잭션으로 일관성 보장
4. **도메인 이벤트 자동 발행**: 부수효과의 자동 처리
5. **타입 안전한 에러 변환**: 도메인 예외를 애플리케이션 에러로 자동 변환
6. **분산 락 지원**: 동시성 제어가 필요한 UseCase에서 쉬운 적용
7. **확장 함수 지원**: 도메인 객체 작업의 편의성 제공

이러한 패턴을 통해 복잡한 비즈니스 로직을 안전하고 일관성 있게 처리할 수 있습니다.