# ⚽ Ball Framework

**Version:** 2.0.0-SNAPSHOT  
**License:** MIT  
**Language:** Kotlin  
**Java Version:** 21+  

Ball Framework는 **헥사고날 아키텍처(Hexagonal Architecture)**와 **도메인 주도 설계(DDD)** 원칙을 기반으로 설계된 엔터프라이즈급 Kotlin/Spring 프레임워크입니다.

## 🎯 주요 특징

- 🏗️ **헥사고날 아키텍처**: 포트와 어댑터 패턴으로 깔끔한 의존성 분리
- 🧩 **도메인 주도 설계**: 비즈니스 로직을 중심으로 한 모듈러 설계
- 🔐 **분산 락**: `@LockKey` 어노테이션 기반의 직관적인 분산 락 시스템
- ⚡ **성능 최적화**: Arrow의 함수형 프로그래밍과 효율적인 에러 처리
- 🧪 **테스트 친화적**: Kotest 기반의 포괄적인 테스트 지원
- 📦 **모듈화**: 독립적으로 사용 가능한 컴포넌트들

## 🏛️ 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│                      Inbound Adapters                       │
│                    (REST, etc.)                             │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│                 (Use Cases, Commands)                       │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│              (Entities, Value Objects, Services)            │
├─────────────────────────────────────────────────────────────┤
│                    Outbound Adapters                        │
│                 (JPA, Redis, External APIs)                 │
└─────────────────────────────────────────────────────────────┘
```

## 📁 프로젝트 구조

```
ball-framework/
├── domain/                     # 도메인 모델 및 비즈니스 규칙
│   ├── model/                  # 엔티티, 값 객체, 집합체 루트
│   ├── exception/              # 도메인 예외 계층
│   ├── port/                   # 아웃바운드 포트 (인터페이스)
│   └── service/                # 도메인 서비스
├── application/                # 애플리케이션 서비스 및 유스케이스
│   └── usecase/                # Use Case 구현체
├── adapter/
│   ├── inbound/
│   │   └── rest/               # REST API 어댑터
│   └── outbound/
│       ├── data-access-core/   # 데이터 접근 추상화
│       ├── data-access-jpa/    # JPA 구현체
│       └── data-access-redis/  # Redis 구현체
├── shared/                     # 공통 유틸리티 및 라이브러리
│   ├── arrow/                  # Arrow 함수형 확장
│   ├── jackson/                # JSON 직렬화 지원
│   └── lock/                   # 분산 락 시스템
└── ball-sdk/                   # SDK 및 클라이언트 라이브러리
```

## 🚀 빠른 시작

### 1. 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.clroot.ball:domain:2.0.0-SNAPSHOT")
    implementation("io.clroot.ball:application:2.0.0-SNAPSHOT")
    implementation("io.clroot.ball:adapter-inbound-rest:2.0.0-SNAPSHOT")
    implementation("io.clroot.ball:adapter-outbound-data-access-jpa:2.0.0-SNAPSHOT")
}
```

### 2. 도메인 엔티티 정의

```kotlin
import io.clroot.ball.domain.model.AggregateRoot
import io.clroot.ball.domain.model.vo.BinaryId
import java.time.Instant

class User(
    id: BinaryId,
    private var name: String,
    private var email: Email,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant? = null
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt) {
    
    fun changeName(newName: String) {
        this.name = newName
        registerEvent(UserNameChangedEvent(id, newName))
    }
}
```

### 3. 유스케이스 구현

```kotlin
import io.clroot.ball.application.usecase.UseCase
import arrow.core.Either
import io.clroot.ball.application.ApplicationError

@Service
class UpdateUserNameUseCase(
    private val userRepository: UserRepository,
    applicationEventPublisher: ApplicationEventPublisher
) : UseCase<UpdateUserNameCommand, User>(applicationEventPublisher) {

    override fun executeInternal(command: UpdateUserNameCommand): User {
        return userRepository.update(command.userId) { user ->
            user.changeName(command.newName)
        }.also { publishEvents(it) }
    }
}
```

### 4. REST 컨트롤러

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(
    private val updateUserNameUseCase: UpdateUserNameUseCase
) {
    
    @PutMapping("/{userId}/name")
    fun updateUserName(
        @PathVariable userId: String,
        @RequestBody request: UpdateUserNameRequest
    ): ResponseEntity<UserResponse> {
        val command = UpdateUserNameCommand(userId, request.name)
        
        return updateUserNameUseCase.execute(command)
            .fold(
                { error -> ResponseEntity.badRequest().build() },
                { user -> ResponseEntity.ok(UserResponse.from(user)) }
            )
    }
}
```

## 🔐 분산 락 사용법

Ball Framework의 강력한 분산 락 시스템을 활용하여 동시성 문제를 해결하세요:

```kotlin
@Service
class UserService {
    
    @DistributedLock(key = "user-{userId}")
    fun updateUser(@LockKey("userId") userId: String, data: UserData) {
        // 동일한 사용자 ID로 동시 업데이트 방지
    }
    
    @DistributedLock(key = "payment-{userId}-{orderId}")
    fun processPayment(
        @LockKey("userId") userId: String,
        @LockKey("orderId") orderId: String
    ) {
        // 사용자별, 주문별 결제 처리 락
    }
}
```

자세한 내용은 [분산 락 가이드](shared/lock/README.md)를 참조하세요.

## 🛠️ 개발 환경 설정

### 요구사항

- **Java**: 21+
- **Kotlin**: 2.1.20+
- **Spring Boot**: 3.4.4+
- **Gradle**: 8.0+

### 빌드 및 테스트

```bash
# 전체 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 특정 모듈 빌드
./gradlew :domain:build

# 코드 스타일 검사
./gradlew ktlintCheck
```

### IDE 설정

IntelliJ IDEA 사용 시 다음 설정을 권장합니다:

1. **Kotlin Code Style**: `gradle.properties`에서 `kotlin.code.style=official` 설정
2. **Annotation Processing**: 활성화 (Spring 어노테이션 처리)
3. **Gradle**: Gradle Wrapper 사용

## 🏗️ 모듈별 가이드

### Domain 모듈

핵심 비즈니스 로직과 도메인 모델을 포함합니다:

- **AggregateRoot**: 도메인 이벤트를 관리하는 집합체 루트
- **EntityBase**: 공통 엔티티 기능 (ID, 생성/수정 시간, 소프트 삭제)
- **ValueObject**: 불변 값 객체 기반 클래스
- **Repository**: 영속성 추상화 인터페이스
- **DomainException**: 도메인별 예외 계층

### Application 모듈

유스케이스와 애플리케이션 서비스를 포함합니다:

- **UseCase**: 트랜잭션과 에러 처리가 내장된 기본 유스케이스 클래스
- **Query**: CQRS 패턴의 조회 작업 추상화
- **ApplicationError**: Arrow Either를 활용한 함수형 에러 처리

### Adapter 모듈

외부 시스템과의 인터페이스를 담당합니다:

#### Inbound Adapters
- **REST**: Spring WebMVC 기반 RESTful API
- **GlobalExceptionHandler**: 통합 예외 처리 및 에러 응답
- **RequestLoggingFilter**: 요청/응답 로깅

#### Outbound Adapters
- **JPA**: Spring Data JPA를 활용한 데이터 접근
- **Redis**: Redis 기반 캐싱 및 분산 락
- **Core**: 데이터 접근 공통 추상화

## 🔧 설정 가이드

### application.yml 예시

```yaml
spring:
  profiles:
    active: local
  
  datasource:
    url: jdbc:postgresql://localhost:5432/ball_db
    username: ${DB_USERNAME:ball_user}
    password: ${DB_PASSWORD:ball_password}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

ball:
  adapter:
    rest:
      enabled: true
  lock:
    provider: redis  # local, redis
    redis:
      wait-time: 10
      lease-time: 30
```

## 🧪 테스트 가이드

Ball Framework는 Kotest를 기반으로 한 포괄적인 테스트를 제공합니다:

```kotlin
class UserServiceTest : FunSpec({
    
    val userRepository = mockk<UserRepository>()
    val userService = UserService(userRepository)
    
    test("사용자 이름 변경 시 도메인 이벤트가 발행되어야 한다") {
        // given
        val userId = BinaryId.generate()
        val user = createUser(userId, "John")
        every { userRepository.findById(userId) } returns user
        every { userRepository.save(any()) } returns user
        
        // when
        userService.changeName(userId, "Jane")
        
        // then
        user.domainEvents shouldHaveSize 1
        user.domainEvents.first() shouldBeInstanceOf UserNameChangedEvent::class
    }
})
```

## 📋 모범 사례

### 1. 도메인 모델링

```kotlin
// ✅ Good: 도메인 로직을 엔티티 내부에 캡슐화
class Order : AggregateRoot<BinaryId>(/* ... */) {
    fun addItem(product: Product, quantity: Int) {
        validateQuantity(quantity)
        validateProductAvailability(product)

        val item = OrderItem(product, quantity)
        items.add(item)
        registerEvent(OrderItemAddedEvent(id, item))
    }
}

// ❌ Bad: 도메인 로직이 서비스에 노출됨
class OrderService {
    fun addItemToOrder(order: Order, product: Product, quantity: Int) {
        if (quantity <= 0) throw InvalidQuantityException()
        // 비즈니스 로직이 서비스에 분산됨
    }
}
```

### 2. 에러 처리

```kotlin
// ✅ Good: Arrow Either를 활용한 함수형 에러 처리
class UserUseCase : UseCase<CreateUserCommand, User> {
    override fun executeInternal(command: CreateUserCommand): User {
        // 도메인 예외는 자동으로 ApplicationError로 변환됨
        return userRepository.save(User.create(command.name, command.email))
    }
}

// 컨트롤러에서 사용
return userUseCase.execute(command)
    .fold(
        { error -> handleError(error) },
        { user -> ResponseEntity.ok(UserResponse.from(user)) }
    )
```

### 3. 분산 락 활용

```kotlin
// ✅ Good: 명시적이고 안전한 락 키 정의
@DistributedLock(key = "inventory-{productId}-{warehouseId}")
fun updateStock(
    @LockKey("productId") productId: String,
    @LockKey("warehouseId") warehouseId: String,
    quantity: Int
) {
    // 재고 업데이트 로직
}
```

## 🤝 기여하기

Ball Framework는 오픈소스 프로젝트입니다. 기여를 환영합니다!

### 기여 방법

1. 이슈 생성 또는 기존 이슈 확인
2. Fork 및 브랜치 생성
3. 코드 작성 및 테스트 추가
4. Pull Request 생성

### 코드 스타일

- **Kotlin Official Style**: `kotlin.code.style=official`
- **KtLint**: 자동 코드 포맷팅 (`./gradlew ktlintFormat`)
- **테스트 커버리지**: 새로운 코드는 적절한 테스트와 함께 제출

## 📚 추가 자료

- [헥사고날 아키텍처 가이드](https://alistair.cockburn.us/hexagonal-architecture/)
- [도메인 주도 설계 참고서](https://domainlanguage.com/ddd/)
- [Arrow 함수형 프로그래밍](https://arrow-kt.io/)
- [Kotest 테스트 프레임워크](https://kotest.io/)

## 📞 지원 및 문의

- **이슈 트래커**: [GitHub Issues](https://github.com/clroot/ball-framework/issues)
- **토론**: [GitHub Discussions](https://github.com/clroot/ball-framework/discussions)
- **이메일**: geonhwan.cha@clroot.io

---

**Ball Framework**로 더 나은 소프트웨어를 만들어보세요! ⚽