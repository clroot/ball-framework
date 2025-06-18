# Ball Framework REST Adapter

Ball Framework의 REST API 인바운드 어댑터로, Spring WebMVC를 기반으로 외부 클라이언트의 HTTP 요청을 처리합니다.

## 목차
1. [모듈 개요](#모듈-개요)
2. [통합 예외 처리](#통합-예외-처리)
3. [Either 확장 함수](#either-확장-함수)
4. [요청 로깅](#요청-로깅)
5. [자동 설정](#자동-설정)
6. [Jackson 설정](#jackson-설정)
7. [사용 예시](#사용-예시)
8. [베스트 프랙티스](#베스트-프랙티스)

## 모듈 개요

REST Adapter는 다음과 같은 패키지 구조로 구성되어 있습니다:

```
adapter/inbound/rest/
├── config/                    # 설정 클래스
│   ├── BallJackson2ObjectMapperBuilderCustomizer.kt
│   └── RestAdapterAutoConfiguration.kt
├── dto/                       # 데이터 전송 객체
│   └── error/                 # 에러 응답 DTO
├── exception/                 # 예외 처리
│   ├── ErrorCodes.kt
│   ├── ExceptionLocationExtractor.kt
│   └── GlobalExceptionHandler.kt
├── extension/                 # 확장 함수
│   └── EitherExtensions.kt
└── filter/                    # 필터
    └── RequestLoggingFilter.kt
```

**주요 의존성:**
- Spring Boot Starter Web
- Spring Boot Starter AOP
- Jakarta Validation API
- Spring Boot Starter Test (테스트용)

## 통합 예외 처리

### GlobalExceptionHandler

모든 예외를 일관성 있게 처리하는 글로벌 예외 핸들러입니다.

```kotlin
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalExceptionHandler {

    @ExceptionHandler(DomainValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDomainValidation(
        exception: DomainValidationException,
        request: HttpServletRequest
    ): ErrorResponse {
        return ErrorResponse.validationError(
            message = exception.message ?: "검증 오류가 발생했습니다",
            field = exception.field,
            code = exception.code,
            path = request.requestURI
        )
    }

    @ExceptionHandler(BusinessRuleException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleBusinessRule(
        exception: BusinessRuleException,
        request: HttpServletRequest
    ): ErrorResponse {
        return ErrorResponse.businessError(
            message = exception.message ?: "비즈니스 규칙 위반입니다",
            ruleCode = exception.ruleCode,
            path = request.requestURI
        )
    }

    @ExceptionHandler(DomainStateException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleDomainState(
        exception: DomainStateException,
        request: HttpServletRequest
    ): ErrorResponse {
        return ErrorResponse.stateError(
            message = exception.message ?: "도메인 상태 오류입니다",
            entityType = exception.entityType,
            entityId = exception.entityId,
            path = request.requestURI
        )
    }

    @ExceptionHandler(ExternalSystemException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleExternalSystem(
        exception: ExternalSystemException,
        request: HttpServletRequest
    ): ErrorResponse {
        return ErrorResponse.externalSystemError(
            message = exception.message ?: "외부 시스템 오류입니다",
            systemName = exception.systemName,
            path = request.requestURI
        )
    }
}
```

### ErrorResponse DTO

```kotlin
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: LocalDateTime,
    val path: String,
    val traceId: String?,
    val details: Map<String, Any>? = null,
    val debugInfo: DebugInfo? = null
) {
    companion object {
        fun validationError(
            message: String,
            field: String? = null,
            code: String? = null,
            path: String
        ): ErrorResponse = ErrorResponse(
            error = "VALIDATION_ERROR",
            message = message,
            timestamp = LocalDateTime.now(),
            path = path,
            traceId = MDC.get("traceId"),
            details = mapOfNotNull(
                "field" to field,
                "code" to code
            ).takeIf { it.isNotEmpty() }
        )

        fun businessError(
            message: String,
            ruleCode: String? = null,
            path: String
        ): ErrorResponse = ErrorResponse(
            error = "BUSINESS_RULE_ERROR",
            message = message,
            timestamp = LocalDateTime.now(),
            path = path,
            traceId = MDC.get("traceId"),
            details = ruleCode?.let { mapOf("ruleCode" to it) }
        )
    }
}
```

### DebugInfo (개발 환경용)

```kotlin
data class DebugInfo(
    val exception: String,
    val stackTrace: List<String>,
    val location: String? = null,
    val causedBy: String? = null
) {
    companion object {
        fun from(
            exception: Throwable,
            includeStackTrace: Boolean = true
        ): DebugInfo {
            return DebugInfo(
                exception = exception::class.qualifiedName ?: "Unknown",
                stackTrace = if (includeStackTrace) {
                    exception.stackTrace.take(10).map { it.toString() }
                } else {
                    emptyList()
                },
                location = ExceptionLocationExtractor.extractLocation(exception),
                causedBy = exception.cause?.let { "${it::class.simpleName}: ${it.message}" }
            )
        }
    }
}
```

## Either 확장 함수

`Either<ApplicationError, T>`를 `ResponseEntity`로 쉽게 변환할 수 있는 확장 함수를 제공합니다.

```kotlin
fun <T> Either<ApplicationError, T>.toResponseEntity(): ResponseEntity<T> {
    return fold(
        ifLeft = { error ->
            when (error) {
                is ApplicationError.DomainError -> {
                    when (val domainException = error.exception) {
                        is DomainValidationException -> ResponseEntity.badRequest().build()
                        is BusinessRuleException -> ResponseEntity.status(HttpStatus.CONFLICT).build()
                        is DomainStateException -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build()
                        is ExternalSystemException -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
                        else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
                is ApplicationError.SystemError -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        },
        ifRight = { value -> ResponseEntity.ok(value) }
    )
}

fun <T, R> Either<ApplicationError, T>.toResponseEntity(
    transform: (T) -> R
): ResponseEntity<R> {
    return map(transform).toResponseEntity()
}

fun <T> Either<ApplicationError, T>.toResponseEntity(
    successStatus: HttpStatus
): ResponseEntity<T> {
    return fold(
        ifLeft = { error -> /* 에러 처리 */ },
        ifRight = { value -> ResponseEntity.status(successStatus).body(value) }
    )
}
```

### 사용 예시

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(
    private val createUserUseCase: CreateUserUseCase,
    private val getUserQuery: GetUserQuery
) {
    
    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val command = CreateUserCommand(
            name = request.name,
            email = request.email,
            age = request.age
        )
        
        return createUserUseCase.execute(command)
            .toResponseEntity(HttpStatus.CREATED) { user ->
                UserResponse.from(user)
            }
    }
    
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: String): ResponseEntity<UserResponse> {
        val query = GetUserByIdQuery(BinaryId.fromString(id))
        
        return getUserQuery.execute(query)
            .toResponseEntity { user ->
                UserResponse.from(user)
            }
    }
}
```

## 요청 로깅

### RequestLoggingFilter

모든 HTTP 요청과 응답을 로깅하는 필터입니다.

```kotlin
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        val traceId = generateTraceId()
        
        // 트레이스 ID를 MDC에 설정
        MDC.put("traceId", traceId)
        response.setHeader("X-Trace-ID", traceId)
        
        try {
            // 요청 로깅
            logRequest(request, traceId)
            
            filterChain.doFilter(request, response)
            
            // 응답 로깅
            logResponse(request, response, traceId, System.currentTimeMillis() - startTime)
            
        } finally {
            MDC.clear()
        }
    }
    
    private fun logRequest(request: HttpServletRequest, traceId: String) {
        if (shouldLog(request)) {
            log.info(
                "HTTP Request - {} {} | TraceId: {} | IP: {} | UserAgent: {}",
                request.method,
                request.requestURI,
                traceId,
                getClientIpAddress(request),
                request.getHeader("User-Agent")
            )
        }
    }
    
    private fun logResponse(
        request: HttpServletRequest,
        response: HttpServletResponse,
        traceId: String,
        duration: Long
    ) {
        if (shouldLog(request)) {
            log.info(
                "HTTP Response - {} {} | Status: {} | Duration: {}ms | TraceId: {}",
                request.method,
                request.requestURI,
                response.status,
                duration,
                traceId
            )
        }
    }
}
```

## 자동 설정

### RestAdapterAutoConfiguration

```kotlin
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(RestAdapterProperties::class)
class RestAdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun globalExceptionHandler(
        properties: RestAdapterProperties
    ): GlobalExceptionHandler {
        return GlobalExceptionHandler(properties.debug)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        name = ["ball.adapter.rest.logging.enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun requestLoggingFilter(): RequestLoggingFilter {
        return RequestLoggingFilter()
    }

    @Bean
    @ConditionalOnMissingBean
    fun ballJackson2ObjectMapperBuilderCustomizer(): BallJackson2ObjectMapperBuilderCustomizer {
        return BallJackson2ObjectMapperBuilderCustomizer()
    }
}
```

### RestAdapterProperties

```kotlin
@ConfigurationProperties(prefix = "ball.adapter.rest")
data class RestAdapterProperties(
    val debug: Boolean = false,
    val logging: LoggingProperties = LoggingProperties()
) {
    data class LoggingProperties(
        val enabled: Boolean = true,
        val includeHeaders: Boolean = false,
        val includePayload: Boolean = false,
        val excludePatterns: List<String> = listOf("/health", "/metrics")
    )
}
```

## Jackson 설정

### BallJackson2ObjectMapperBuilderCustomizer

```kotlin
@Component
class BallJackson2ObjectMapperBuilderCustomizer : Jackson2ObjectMapperBuilderCustomizer {

    override fun customize(builder: Jackson2ObjectMapperBuilder) {
        builder.apply {
            // 날짜/시간 처리
            simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            timeZone(TimeZone.getDefault())
            
            // Kotlin 지원
            modulesToInstall(KotlinModule.Builder().build())
            
            // JSR310 (Java 8 Time API) 지원
            modulesToInstall(JavaTimeModule())
            
            // Arrow 지원
            modulesToInstall(ArrowModule())
            
            // Null 필드 제외
            serializationInclusion(JsonInclude.Include.NON_NULL)
            
            // 알 수 없는 프로퍼티 무시
            featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            
            // 빈 Bean 직렬화 실패 방지
            featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            
            // ISO 날짜 형식 사용
            featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
```

## 사용 예시

### 기본 컨트롤러 구현

```kotlin
@RestController
@RequestMapping("/api/orders")
@Validated
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrderQuery: GetOrderQuery,
    private val updateOrderUseCase: UpdateOrderUseCase
) {
    
    @PostMapping
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val command = CreateOrderCommand(
            userId = request.userId,
            items = request.items.map { item ->
                OrderItemData(
                    productId = item.productId,
                    productName = item.productName,
                    price = Money(item.price),
                    quantity = item.quantity
                )
            }
        )
        
        return createOrderUseCase.execute(command)
            .toResponseEntity(HttpStatus.CREATED) { order ->
                OrderResponse.from(order)
            }
    }
    
    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: String): ResponseEntity<OrderResponse> {
        val query = GetOrderByIdQuery(BinaryId.fromString(id))
        
        return getOrderQuery.execute(query)
            .fold(
                ifLeft = { error ->
                    when (error) {
                        is ApplicationError.DomainError -> {
                            if (error.exception is DomainStateException) {
                                ResponseEntity.notFound().build()
                            } else {
                                ResponseEntity.badRequest().build()
                            }
                        }
                        else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                },
                ifRight = { order ->
                    if (order == null) {
                        ResponseEntity.notFound().build()
                    } else {
                        ResponseEntity.ok(OrderResponse.from(order))
                    }
                }
            )
    }
    
    @PutMapping("/{id}/status")
    fun updateOrderStatus(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateOrderStatusRequest
    ): ResponseEntity<OrderResponse> {
        val command = UpdateOrderStatusCommand(
            orderId = BinaryId.fromString(id),
            status = request.status
        )
        
        return updateOrderUseCase.execute(command)
            .toResponseEntity { order ->
                OrderResponse.from(order)
            }
    }
}
```

### DTO 정의

```kotlin
// 요청 DTO
data class CreateOrderRequest(
    @field:NotNull(message = "사용자 ID는 필수입니다")
    val userId: BinaryId,
    
    @field:NotEmpty(message = "주문 항목은 필수입니다")
    @field:Valid
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    @field:NotNull(message = "상품 ID는 필수입니다")
    val productId: BinaryId,
    
    @field:NotBlank(message = "상품명은 필수입니다")
    val productName: String,
    
    @field:DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다")
    val price: BigDecimal,
    
    @field:Min(value = 1, message = "수량은 1 이상이어야 합니다")
    val quantity: Int
)

// 응답 DTO
data class OrderResponse(
    val id: String,
    val userId: String,
    val items: List<OrderItemResponse>,
    val status: OrderStatus,
    val totalAmount: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(order: Order): OrderResponse {
            return OrderResponse(
                id = order.id.toString(),
                userId = order.userId.toString(),
                items = order.items.map { OrderItemResponse.from(it) },
                status = order.status,
                totalAmount = order.totalAmount.amount,
                createdAt = order.createdAt,
                updatedAt = order.updatedAt
            )
        }
    }
}
```

## 베스트 프랙티스

### 1. Either 확장 함수 활용

```kotlin
// ✅ Good: Either 확장 함수 사용
@PostMapping
fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
    return createUserUseCase.execute(command)
        .toResponseEntity(HttpStatus.CREATED) { user ->
            UserResponse.from(user)
        }
}

// ❌ Bad: 수동 Either 처리
@PostMapping
fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
    val result = createUserUseCase.execute(command)
    return when (result) {
        is Either.Right -> ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(result.value))
        is Either.Left -> {
            when (result.value) {
                is ApplicationError.DomainError -> ResponseEntity.badRequest().build()
                else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }
}
```

### 2. 글로벌 예외 처리 활용

```kotlin
// ✅ Good: 도메인 예외를 그대로 던져서 GlobalExceptionHandler가 처리
@PostMapping
fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
    return createUserUseCase.execute(command)
        .fold(
            ifLeft = { error ->
                when (error) {
                    is ApplicationError.DomainError -> throw error.exception
                    is ApplicationError.SystemError -> throw error.exception
                }
            },
            ifRight = { user -> ResponseEntity.ok(UserResponse.from(user)) }
        )
}

// ❌ Bad: 컨트롤러에서 직접 예외 처리
@PostMapping
fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<Any> {
    return createUserUseCase.execute(command)
        .fold(
            ifLeft = { error ->
                ResponseEntity.badRequest().body(
                    mapOf("error" to error.message)
                )
            },
            ifRight = { user -> ResponseEntity.ok(UserResponse.from(user)) }
        )
}
```

### 3. 요청/응답 DTO 검증

```kotlin
// ✅ Good: Bean Validation 활용
data class CreateUserRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    val name: String,
    
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,
    
    @field:Min(value = 0, message = "나이는 0 이상이어야 합니다")
    @field:Max(value = 150, message = "나이는 150 이하여야 합니다")
    val age: Int
)

// ❌ Bad: 검증 로직 없음
data class CreateUserRequest(
    val name: String,
    val email: String,
    val age: Int
)
```

### 4. 트레이스 ID 활용

```kotlin
// ✅ Good: 로그에 트레이스 ID 포함
@GetMapping("/{id}")
fun getUser(@PathVariable id: String): ResponseEntity<UserResponse> {
    val traceId = MDC.get("traceId")
    log.info("사용자 조회 요청 - id: {}, traceId: {}", id, traceId)
    
    return getUserQuery.execute(GetUserByIdQuery(BinaryId.fromString(id)))
        .toResponseEntity { user -> UserResponse.from(user) }
}

// 클라이언트에서 트레이스 ID 확인 가능
// Response Header: X-Trace-ID: abc123def456
```

### 5. 페이징 처리

```kotlin
@GetMapping
fun getUsers(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
    @RequestParam(defaultValue = "createdAt") sort: String,
    @RequestParam(defaultValue = "desc") direction: String,
    @RequestParam(required = false) name: String?,
    @RequestParam(required = false) status: UserStatus?
): ResponseEntity<PageResponse<UserResponse>> {
    val pageRequest = PageRequest(
        page = page,
        size = size,
        sort = Sort.by(sort).let { 
            if (direction == "desc") it.descending() else it.ascending()
        }
    )
    
    val query = GetUsersQuery(
        pageRequest = pageRequest,
        namePattern = name,
        status = status
    )
    
    return getUsersQuery.execute(query)
        .toResponseEntity { userPage ->
            PageResponse.from(userPage) { user -> UserResponse.from(user) }
        }
}
```

Ball Framework의 REST Adapter는 Spring WebMVC의 강력함과 함수형 에러 처리의 안전성을 결합하여, 견고하고 유지보수하기 쉬운 REST API를 구축할 수 있게 해줍니다.