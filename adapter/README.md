# Ball Framework Adapter Modules

Ball Framework의 어댑터 레이어 모듈들로, 헥사고날 아키텍처의 포트(Port)와 어댑터(Adapter) 패턴을 구현합니다. 외부 시스템과의 인터페이스를 담당하며, 인바운드(Inbound)와 아웃바운드(Outbound) 어댑터로 구성됩니다.

## 목차
1. [어댑터 개요](#어댑터-개요)
2. [Inbound Adapters](#inbound-adapters)
3. [Outbound Adapters](#outbound-adapters)
4. [어댑터 설계 원칙](#어댑터-설계-원칙)

## 어댑터 개요

```
adapter/
├── inbound/                    # 인바운드 어댑터
│   └── rest/                  # REST API 어댑터
└── outbound/                  # 아웃바운드 어댑터
    ├── data-access-core/      # 데이터 접근 코어
    ├── data-access-jpa/       # JPA 데이터 접근
    └── data-access-redis/     # Redis 데이터 접근
```

**헥사고날 아키텍처에서의 역할:**
- **Inbound Adapters**: 외부에서 애플리케이션으로 들어오는 요청을 처리 (Primary Adapters)
- **Outbound Adapters**: 애플리케이션에서 외부 시스템으로 나가는 요청을 처리 (Secondary Adapters)
- **포트 구현**: 도메인에 정의된 포트(인터페이스)를 구체적으로 구현

## Inbound Adapters

### REST Adapter

REST API를 통해 외부 클라이언트의 요청을 받아 애플리케이션 레이어로 전달하는 어댑터입니다.

**위치**: `adapter/inbound/rest/`

자세한 내용은 [REST Adapter README](inbound/rest/README.md)를 참조하세요.

## Outbound Adapters

### Data Access Core

데이터 접근 어댑터들의 공통 기능을 제공하는 코어 모듈입니다.

**위치**: `adapter/outbound/data-access-core/`

자세한 내용은 [Data Access Core README](outbound/data-access-core/README.md)를 참조하세요.

### Data Access JPA

JPA(Java Persistence API)를 사용하여 관계형 데이터베이스에 접근하는 어댑터입니다.

**위치**: `adapter/outbound/data-access-jpa/`

자세한 내용은 [Data Access JPA README](outbound/data-access-jpa/README.md)를 참조하세요.

### Data Access Redis

Redis를 사용하여 캐싱 및 분산 락 기능을 제공하는 어댑터입니다.

**위치**: `adapter/outbound/data-access-redis/`

자세한 내용은 [Data Access Redis README](outbound/data-access-redis/README.md)를 참조하세요.

## 어댑터 설계 원칙

### 1. 의존성 역전 원칙

```kotlin
// 도메인 포트 정의 (domain/port/)
interface UserRepository : Repository<User, BinaryId> {
    fun findByEmail(email: Email): User?
    fun existsByEmail(email: Email): Boolean
}

// 어댑터 구현 (adapter/outbound/)
@Repository
class UserJpaAdapter(
    private val jpaRepository: UserJpaRepository
) : JpaRepositoryAdapter<User, BinaryId, UserJpaRecord>(jpaRepository), UserRepository {
    
    override fun findByEmail(email: Email): User? {
        return jpaRepository.findByEmail(email.value)?.toDomain()
    }
    
    override fun existsByEmail(email: Email): Boolean {
        return jpaRepository.existsByEmail(email.value)
    }
}
```

### 2. 포트와 어댑터 분리

```kotlin
// 포트 (도메인에 정의)
interface NotificationPort {
    fun sendEmail(to: Email, subject: String, content: String): Either<NotificationError, Unit>
    fun sendSms(to: PhoneNumber, message: String): Either<NotificationError, Unit>
}

// 어댑터 (인프라에 구현)
@Component
class EmailNotificationAdapter(
    private val emailService: EmailService
) : NotificationPort {
    
    override fun sendEmail(to: Email, subject: String, content: String): Either<NotificationError, Unit> {
        return try {
            emailService.send(to.value, subject, content)
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(NotificationError.EmailSendFailed(e.message))
        }
    }
    
    override fun sendSms(to: PhoneNumber, message: String): Either<NotificationError, Unit> {
        // SMS 발송 로직
        return Either.Right(Unit)
    }
}
```

### 3. 어댑터 간 독립성

```kotlin
// 각 어댑터는 독립적으로 사용 가능
@SpringBootApplication
class Application {
    // JPA만 사용하는 설정
    @Profile("jpa-only")
    @Configuration
    class JpaOnlyConfiguration
    
    // Redis와 JPA 함께 사용하는 설정
    @Profile("full-stack")
    @Configuration
    class FullStackConfiguration
}
```

### 4. 자동 설정 지원

```kotlin
// 각 어댑터는 Spring Boot Auto Configuration 제공
@AutoConfiguration
@ConditionalOnProperty(name = "ball.adapter.rest.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RestAdapterProperties::class)
class RestAdapterAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    fun globalExceptionHandler(): GlobalExceptionHandler {
        return GlobalExceptionHandler()
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun requestLoggingFilter(): RequestLoggingFilter {
        return RequestLoggingFilter()
    }
}
```

### 5. 에러 변환

```kotlin
// 인프라 예외를 도메인 예외로 변환
@Repository
class UserJpaAdapter : JpaRepositoryAdapter<User, BinaryId, UserJpaRecord>, UserRepository {
    
    override fun save(entity: User): User {
        return try {
            super.save(entity)
        } catch (e: DataIntegrityViolationException) {
            throw DuplicateEntityException("이미 존재하는 사용자입니다", e)
        } catch (e: Exception) {
            throw PersistenceException("사용자 저장 중 오류가 발생했습니다", e)
        }
    }
}
```

## 어댑터 확장 가이드

### 새로운 인바운드 어댑터 추가

1. **어댑터 모듈 생성**
   ```kotlin
   // adapter/inbound/graphql/ 예시
   build.gradle.kts
   src/main/kotlin/...
   src/test/kotlin/...
   ```

2. **컨트롤러 구현**
   ```kotlin
   @GraphQLController
   class UserGraphQLController(
       private val createUserUseCase: CreateUserUseCase,
       private val getUserQuery: GetUserQuery
   ) {
       @QueryMapping
       fun user(@Argument id: String): User? {
           return getUserQuery.execute(GetUserByIdQuery(BinaryId.fromString(id)))
               .fold(
                   ifLeft = { null },
                   ifRight = { it }
               )
       }
   }
   ```

3. **자동 설정 구성**
   ```kotlin
   @AutoConfiguration
   @ConditionalOnClass(GraphQL::class)
   class GraphQLAdapterAutoConfiguration {
       // GraphQL 설정
   }
   ```

### 새로운 아웃바운드 어댑터 추가

1. **포트 정의** (도메인에)
   ```kotlin
   interface MessageQueuePort {
       fun publishMessage(topic: String, message: Any): Either<MessageError, Unit>
       fun subscribeToTopic(topic: String, handler: (Any) -> Unit): Either<MessageError, Unit>
   }
   ```

2. **어댑터 구현**
   ```kotlin
   @Component
   class KafkaMessageQueueAdapter(
       private val kafkaTemplate: KafkaTemplate<String, Any>
   ) : MessageQueuePort {
       
       override fun publishMessage(topic: String, message: Any): Either<MessageError, Unit> {
           return try {
               kafkaTemplate.send(topic, message)
               Either.Right(Unit)
           } catch (e: Exception) {
               Either.Left(MessageError.PublishFailed(e.message))
           }
       }
   }
   ```

3. **자동 설정 추가**
   ```kotlin
   @AutoConfiguration
   @ConditionalOnProperty(name = "ball.adapter.kafka.enabled", havingValue = "true")
   class KafkaAdapterAutoConfiguration {
       // Kafka 설정
   }
   ```

## 테스트 전략

### 어댑터 테스트

```kotlin
// 인바운드 어댑터 테스트
@SpringBootTest
@TestPropertySource(properties = ["ball.adapter.rest.debug=true"])
class UserControllerTest {
    
    @MockBean
    private lateinit var createUserUseCase: CreateUserUseCase
    
    @Test
    fun `사용자 생성 API 테스트`() {
        // given
        val command = CreateUserCommand("John", "john@example.com", 25)
        val user = User(BinaryId.new(), "John", Email("john@example.com"), 25)
        
        given(createUserUseCase.execute(command))
            .willReturn(Either.Right(user))
        
        // when & then
        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserRequest.from(command)))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.name").value("John"))
    }
}

// 아웃바운드 어댑터 테스트
@DataJpaTest
class UserJpaAdapterTest {
    
    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository
    
    private lateinit var userJpaAdapter: UserJpaAdapter
    
    @BeforeEach
    fun setUp() {
        userJpaAdapter = UserJpaAdapter(userJpaRepository)
    }
    
    @Test
    fun `사용자 저장 테스트`() {
        // given
        val user = User(BinaryId.new(), "John", Email("john@example.com"), 25)
        
        // when
        val savedUser = userJpaAdapter.save(user)
        
        // then
        assertThat(savedUser.id).isEqualTo(user.id)
        assertThat(savedUser.name).isEqualTo("John")
    }
}
```

### 통합 테스트

```kotlin
@SpringBootTest
@TestPropertySource(properties = [
    "ball.adapter.jpa.enabled=true",
    "ball.adapter.redis.enabled=true"
])
class AdapterIntegrationTest {
    
    @Test
    fun `전체 플로우 통합 테스트`() {
        // REST → Application → Domain → JPA/Redis 전체 플로우 테스트
    }
}
```

Ball Framework의 어댑터 모듈들은 유연하고 확장 가능한 아키텍처를 제공하며, 각 어댑터는 독립적으로 사용할 수 있도록 설계되었습니다.