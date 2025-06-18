# Ball Framework Shared Modules

Ball Framework의 공유 모듈들로, 프레임워크 전체에서 재사용 가능한 기능들을 제공합니다.

## 목차
1. [모듈 개요](#모듈-개요)
2. [Arrow 모듈](#arrow-모듈)
3. [Jackson 모듈](#jackson-모듈)
4. [Lock 모듈](#lock-모듈)

## 모듈 개요

```
shared/
├── arrow/              # Arrow 함수형 프로그래밍 확장
├── jackson/            # Jackson JSON 직렬화 지원
└── lock/               # 분산 락 기능
```

**설계 원칙:**
- **독립성**: 각 모듈은 독립적으로 사용 가능
- **재사용성**: 프레임워크 전체에서 공통으로 사용
- **확장성**: 필요에 따라 새로운 공유 모듈 추가 가능
- **의존성 최소화**: 필수 의존성만 포함

## Arrow 모듈

함수형 프로그래밍을 위한 Arrow 라이브러리 확장 기능을 제공합니다.

**위치**: `shared/arrow/`

### 주요 기능

```kotlin
// flatMapIf: 조건부 flatMap
fun <A, B> Either<A, B>.flatMapIf(
    condition: (B) -> Boolean,
    transform: (B) -> Either<A, B>
): Either<A, B> {
    return flatMap { value ->
        if (condition(value)) {
            transform(value)
        } else {
            Either.Right(value)
        }
    }
}
```

### 사용 예시

```kotlin
// 조건부 변환
val result = createUserUseCase.execute(command)
    .flatMapIf(
        condition = { user -> user.age < 18 },
        transform = { user -> 
            sendParentalConsentEmail(user)
                .map { user }
        }
    )
```

**의존성:**
- Arrow Core 2.0.1

## Jackson 모듈

JSON 직렬화/역직렬화를 위한 Jackson 설정과 확장 기능을 제공합니다.

**위치**: `shared/jackson/`

### 주요 기능

- Kotlin 모듈 지원
- JSR310 (Java 8 Time API) 지원
- Arrow 타입 직렬화 지원
- 커스텀 직렬화 설정

### 지원되는 타입

```kotlin
// 기본 타입들
data class User(
    val id: BinaryId,                    // 커스텀 ID 타입
    val email: Email,                    // 값 객체
    val createdAt: LocalDateTime,        // JSR310 날짜/시간
    val preferences: Map<String, Any>    // 동적 JSON
)

// Either 타입 직렬화
val response: Either<ApplicationError, User> = useCase.execute(command)
// JSON으로 자동 변환
```

**의존성:**
- Jackson Module Kotlin
- Jackson Datatype JSR310
- Arrow Integrations Jackson Module

## Lock 모듈

분산 락 기능을 제공하는 모듈입니다.

**위치**: `shared/lock/`

자세한 내용은 [Lock Module README](lock/README.md)를 참조하세요.

### 주요 기능

- `@DistributedLock` 어노테이션
- `@LockKey` 어노테이션으로 명시적 락 키 정의
- 다양한 LockProvider 구현체 지원
- 로컬 개발용 LocalLockProvider 제공

### 사용 예시

```kotlin
@Service
class UserService {
    
    @DistributedLock(key = "user-update-{userId}")
    fun updateUser(@LockKey("userId") userId: String, data: UserData) {
        // 동일 사용자에 대한 동시 업데이트 방지
    }
}
```

**의존성:**
- Spring Boot Starter AOP

## 새로운 공유 모듈 추가 가이드

### 1. 모듈 생성

```bash
mkdir shared/새모듈명
cd shared/새모듈명
```

### 2. build.gradle.kts 작성

```kotlin
plugins {
    id("ball.kotlin-library-conventions")
}

dependencies {
    // 필요한 의존성만 추가
    api("org.springframework.boot:spring-boot-starter")
    
    // 테스트 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

### 3. 기본 구조 생성

```
shared/새모듈명/
├── build.gradle.kts
├── README.md
└── src/
    ├── main/
    │   ├── kotlin/
    │   │   └── io/clroot/ball/shared/새모듈명/
    │   └── resources/
    └── test/
        ├── kotlin/
        └── resources/
```

### 4. 메인 모듈에서 참조

```kotlin
// shared/build.gradle.kts
dependencies {
    api(project(":shared:새모듈명"))
}
```

### 5. Auto Configuration (필요한 경우)

```kotlin
@AutoConfiguration
@ConditionalOnProperty(name = ["ball.shared.새모듈명.enabled"], havingValue = "true", matchIfMissing = true)
class 새모듈명AutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    fun 새모듈명Component(): 새모듈명Component {
        return 새모듈명Component()
    }
}
```

### 6. META-INF/spring 설정

```
# src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.clroot.ball.shared.새모듈명.config.새모듈명AutoConfiguration
```

## 공유 모듈 설계 원칙

### 1. 단일 책임 원칙

```kotlin
// ✅ Good: 명확한 단일 기능
// shared/encryption/ - 암호화 기능만
// shared/cache/ - 캐싱 기능만
// shared/validation/ - 검증 기능만

// ❌ Bad: 여러 기능 혼재
// shared/utils/ - 암호화, 캐싱, 검증 모두 포함
```

### 2. 의존성 최소화

```kotlin
// ✅ Good: 필요한 의존성만
dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("com.fasterxml.jackson.core:jackson-core")
}

// ❌ Bad: 불필요한 의존성 포함
dependencies {
    api("org.springframework.boot:spring-boot-starter-web")  // 웹 기능 불필요
    api("org.springframework.boot:spring-boot-starter-data-jpa")  // DB 기능 불필요
}
```

### 3. 설정 가능성

```kotlin
// 사용자가 커스터마이징 할 수 있도록
@ConfigurationProperties(prefix = "ball.shared.새모듈명")
data class 새모듈명Properties(
    val enabled: Boolean = true,
    val timeout: Duration = Duration.ofSeconds(30),
    val maxRetries: Int = 3
)
```

### 4. 테스트 지원

```kotlin
// 테스트용 구현체 제공
@TestConfiguration
class 새모듈명TestConfiguration {
    
    @Bean
    @Primary
    fun test새모듈명Component(): 새모듈명Component {
        return Mock새모듈명Component()
    }
}
```

## 베스트 프랙티스

### 1. 패키지 구조

```
shared/모듈명/
└── src/main/kotlin/io/clroot/ball/shared/모듈명/
    ├── config/          # 자동 설정
    ├── exception/       # 모듈 특화 예외
    ├── extension/       # 확장 함수
    └── 핵심기능.kt       # 메인 기능
```

### 2. API 설계

```kotlin
// ✅ Good: 간단하고 직관적인 API
interface CacheManager {
    fun <T> get(key: String, type: Class<T>): T?
    fun put(key: String, value: Any, ttl: Duration = Duration.ofMinutes(30))
    fun remove(key: String)
}

// ❌ Bad: 복잡한 API
interface CacheManager {
    fun <T> getWithOptions(
        key: String, 
        type: Class<T>, 
        options: CacheOptions = CacheOptions.builder().build()
    ): CacheResult<T>
}
```

### 3. 오류 처리

```kotlin
// 공유 모듈 전용 예외 계층
abstract class SharedModuleException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class CacheException(message: String, cause: Throwable? = null) : SharedModuleException(message, cause)
class LockException(message: String, cause: Throwable? = null) : SharedModuleException(message, cause)
```

### 4. 로깅

```kotlin
// 모듈별 로거 사용
class LockProvider {
    companion object {
        private val log = LoggerFactory.getLogger(LockProvider::class.java)
    }
    
    fun acquireLock(key: String): Lock? {
        log.debug("분산 락 획득 시도: key={}", key)
        // ...
    }
}
```

Ball Framework의 공유 모듈들은 재사용 가능하고 확장 가능한 구조로 설계되어, 프레임워크 전체의 일관성과 개발 효율성을 높입니다.