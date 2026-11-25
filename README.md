# Spring Batch Skeleton

Spring Batch 6와 Spring Boot 4로 만든 멀티 모듈 배치 템플릿입니다.
잡 메타 DB와 업무 DB를 분리한 구성, 재시작에 안전한 샘플 잡, AI 코딩 에이전트용 지침(`AGENTS.md`)과 검증 훅을 담고 있습니다.

GitHub에서 **Use this template**으로 새 저장소를 만들어 시작하세요.

## 요구 사항

| 항목 | 버전 |
|---|---|
| JDK | 25 (빌드와 실행 모두) |
| Gradle | 9.5.1 (wrapper 포함) |
| Spring Boot | 4.1.2 (`gradle.properties`) |
| Spring Batch | 6.0.6 (Spring Boot BOM이 관리) |
| jq | 훅 스크립트 `check-java.sh`에서 사용 (선택) |

- 로컬에 JDK 25가 없으면 Gradle이 foojay toolchain resolver로 내려받습니다.
- 25로 컴파일한 클래스는 더 낮은 JDK에서 실행되지 않습니다. 실행 서버의 JDK도 25 이상이어야 합니다.

## 모듈 구조

```
.
├── AGENTS.md                 # AI 코딩 에이전트 지침
├── .claude/settings.json     # Claude Code 훅 등록 (로직은 scripts/)
├── .github/workflows/        # CI: ./gradlew build
├── scripts/                  # 훅이 호출하는 검증 스크립트
├── buildSrc/                 # 모든 모듈 공통 Gradle 플러그인
├── gradle.properties         # Spring Boot 버전 (한 곳에서만 관리)
├── batch-support/            # 배치 인프라, 잡 메타 DB, JobService
├── main-db-support/          # 업무 DB 데이터 소스와 트랜잭션 관리자
├── sample-job-group/         # 샘플 잡 그룹 (CSV 파일 → 업무 DB)
└── admin-cli/                # Spring Boot 실행 jar
```

의존 방향은 위에서 아래로 한 방향입니다.

```
admin-cli
  └── sample-job-group          (잡 그룹)
        ├── batch-support       (api)
        └── main-db-support     (implementation, 업무 DB를 쓰는 그룹만)
```

- **잡 그룹**: 관련된 잡을 묶은 Gradle 모듈입니다. `SampleJobGroup`이 `@Import`로 잡 설정과 그 그룹이 쓰는 DB 설정을 모으고, `SampleJobGroupBootstrap`이 여기에 배치 인프라를 더해 실행 컨텍스트를 만듭니다.
- **컨텍스트 범위**: 잡 그룹 모듈을 직접 실행하면 그 그룹의 잡만 컨텍스트에 올라갑니다. `admin-cli`는 자신이 `@Import`한 모든 그룹을 한 컨텍스트에 올립니다.
- **스프링 부트**: `admin-cli`에만 의존합니다. 나머지 모듈은 Spring Batch와 Spring Framework만 씁니다.

## 빠른 시작

```bash
./gradlew build

mkdir -p input
printf 'a,100\nb,0\nc,300\n' > input/2026-11-25.csv
```

잡은 두 가지 방법으로 실행합니다.

**1. 잡 그룹 모듈을 `CommandLineJobOperator`로 실행**

```bash
java -cp "sample-job-group/build/libs/*" \
  org.springframework.batch.core.launch.support.CommandLineJobOperator \
  com.example.batch.sample.SampleJobGroupBootstrap \
  start sampleJob targetDate=2026-11-25,java.time.LocalDate
```

- `build`가 `copyDependencies` 태스크로 실행에 필요한 jar를 `sample-job-group/build/libs`에 모읍니다.
- 다른 오퍼레이션은 잡 이름 대신 잡 실행 ID를 받습니다: `stop <jobExecutionId>`, `restart <jobExecutionId>`, `abandon <jobExecutionId>`, `recover <jobExecutionId>`.
- 다른 프로세스에서 `stop`, `restart`를 하려면 잡 메타 DB가 영속 DB여야 합니다. 기본값인 H2 메모리 DB는 프로세스가 끝나면 사라집니다.
- 이 경로는 잡이 끝나면 컨텍스트를 닫지 않고 바로 종료합니다. 커넥션 풀 종료 로그가 남지 않는 것은 정상입니다.

**2. `admin-cli` 실행 jar로 실행**

```bash
java -jar admin-cli/build/libs/admin-cli-0.1.0.jar --job.name=sampleJob \
  targetDate=2026-11-25,java.time.LocalDate
```

- 잡 이름은 `--job.name=<이름>` 또는 `-Djob.name=<이름>`으로 넘깁니다.
- 나머지 인자(`이름=값`)는 잡 파라미터가 됩니다.

### 잡 파라미터 형식

`이름=값,타입` 형식입니다. 타입을 생략하면 문자열로 전달됩니다.

샘플 잡의 `targetDate`는 `java.time.LocalDate`로 넘기세요. 타입을 생략했을 때 `admin-cli`는 Spring Boot의 변환 서비스로 날짜를 읽지만, `CommandLineJobOperator` 경로는 변환하지 못해 잡이 실패합니다.

### 종료 코드

| 종료 코드 | CommandLineJobOperator | admin-cli |
|---|---|---|
| 0 | 잡이 `COMPLETED` | 잡이 `COMPLETED` |
| 1 | 그 밖의 종료 상태, 등록되지 않은 잡 이름 | 그 밖의 종료 상태, 필수 파라미터 누락이나 잘못된 인자 같은 예외 |
| 2 | 잡 구성 클래스를 찾지 못함 | 잡 이름이 없거나 등록되지 않은 이름 |

## 설정

| 파일 | 모듈 | 내용 |
|---|---|---|
| `job-db.properties` | batch-support | 잡 메타 DB 접속 정보, 스키마 초기화 여부 |
| `main-db.properties` | main-db-support | 업무 DB 접속 정보, 스키마 초기화 여부 |
| `sample-job.properties` | sample-job-group | 입력 파일 위치, 청크 크기 |
| `logback.xml` | batch-support | 로그 형식과 레벨 |

기본값은 두 DB 모두 H2 메모리 DB입니다. 프로세스가 끝나면 데이터가 사라집니다.

`logback.xml`은 두 실행 경로가 함께 쓰도록 `batch-support`에 하나만 둡니다. 실행 모듈마다 로그 설정을 따로 두려면 이 파일을 옮기세요. 클래스패스에 `logback.xml`이 둘 이상 있으면 Logback이 경고를 내고 그중 하나를 고릅니다.

시스템 속성과 환경 변수가 속성 파일보다 우선합니다. `admin-cli`에서는 `--이름=값` 형식의 명령행 인자도 씁니다.

```bash
java -Djob-db.jdbc-url=jdbc:postgresql://meta-host/batch -cp ...
MAIN_DB_JDBC_URL=jdbc:postgresql://main-host/app java -jar ...
```

### 운영 DB로 바꿀 때

> [!WARNING]
> 접속 주소만 바꾸면 기본 설정(`initialize-schema=true`, H2용 스키마 스크립트)이 운영 DB에 그대로 실행됩니다. 접속 주소와 함께 아래 값도 바꾸세요.

1. 각 support 모듈의 `runtimeOnly 'com.h2database:h2'`를 운영 DB의 JDBC 드라이버로 바꿉니다.
2. `job-db.initialize-schema=false`, `main-db.initialize-schema=false`로 두고 스키마를 별도로 관리합니다.
3. 잡 메타 DB 스키마는 `spring-batch-core`에 들어 있는 `org/springframework/batch/core/schema-<db>.sql`을 씁니다(`mysql`, `postgresql`, `oracle`, `sqlserver` 등). 자동 초기화를 계속 쓰려면 `job-db.schema-script`를 이 스크립트로 바꿉니다.
4. `main-db-schema-local.sql`은 로컬과 테스트용 샘플 테이블입니다.

## 새 잡 추가하기

1. 잡의 성격에 맞는 잡 그룹 모듈에 `XxxJobConfig`를 만듭니다. 잡 이름은 상수로 선언합니다.
2. 그 그룹의 `*JobGroup` 클래스의 `@Import` 목록에 추가합니다.
3. 업무 DB에 쓰는 스텝이면 아래 "재시작 전략"을 따릅니다.
4. 결과 데이터까지 단언하는 잡 테스트를 추가합니다. 재시작을 다루면 실패 후 재시작 테스트도 추가합니다.

새 잡 그룹이 필요하면 `sample-job-group`을 복사해 `settings.gradle`에 추가합니다. 그런 다음 `admin-cli`의 의존성과 `AdminCliApplication`의 `@Import`에 그 그룹을 더합니다.

## 설계 결정

### 잡 메타 DB와 업무 DB 분리

| 용어 | 뜻 | 빈 |
|---|---|---|
| 잡 메타 DB | Spring Batch가 잡 실행 이력과 상태(`BATCH_*` 테이블)를 저장하는 DB | `jobMetaDataSource`, `jobMetaTransactionManager` |
| 업무 DB | 잡이 읽고 쓰는 업무 데이터가 있는 DB | `mainDataSource`, `mainTransactionManager` |

업무 DB 설정은 `batch-support`가 아니라 별도 모듈(`main-db-support`)에 둡니다.

- `batch-support`의 책임을 배치 인프라로 한정합니다.
- 업무 DB를 쓰지 않는 잡 그룹은 업무 DB 설정과 커넥션 풀 의존을 받지 않습니다.
- 업무 DB가 늘어나면 support 모듈을 추가하고, 공통 모듈은 고치지 않습니다.
- 여러 그룹이 같은 업무 DB를 써도 잡 그룹 사이의 의존 없이 설정을 공유합니다.

`JobOperator`와 `JobRepository`는 모두 `jobMetaTransactionManager`를 씁니다.

### 재시작 전략

DB를 나누면 청크 하나의 처리 순서가 다음과 같습니다.

```
ChunkOrientedStep.doExecute()
└─ 청크 트랜잭션 시작 (mainTransactionManager, 업무 DB)
    ├─ read → process → write                 업무 DB에 쓰기 (커밋 전)
    └─ saveState()
        ├─ jobRepository.updateExecutionContext()   ① 잡 메타 DB 커밋
        └─ jobRepository.update()                   ② 잡 메타 DB 커밋
           (jobMetaTransactionManager, PROPAGATION_REQUIRED:
            업무 DB 트랜잭션에 합류하지 않고 호출마다 새 트랜잭션으로 커밋)
└─ 청크 트랜잭션 커밋                                 ③ 업무 DB 커밋
```

②와 ③ 사이에 프로세스가 죽으면, 잡 메타 DB에는 그 청크가 처리된 것으로 남고 업무 데이터는 롤백됩니다. 리더가 위치를 저장했다면 재시작은 그 청크를 건너뜁니다.

그래서 업무 DB에 쓰는 스텝은 처음부터 다시 처리하는 방식으로 만듭니다.

- 리더는 `saveState(false)`로 두어 재시작 위치를 저장하지 않습니다.
- 첫 스텝(`clearTargetDateStep`)이 대상일 데이터를 지우고, `allowStartIfComplete(true)`로 재시작할 때도 다시 실행됩니다.
- 그래서 재시작은 대상일 전체를 다시 적재하고, 누락과 중복이 생기지 않습니다. `SampleJobTest`의 재시작 테스트가 이 동작을 검증합니다.

잡 메타 DB와 업무 DB가 같은 DB이고 같은 트랜잭션 관리자를 쓰면 ①, ②, ③이 하나의 트랜잭션으로 묶입니다. 그 경우에는 리더 상태를 저장해 중간부터 재시작하는 방식도 쓸 수 있습니다.

커밋 결과를 알 수 없는 경우(커밋 중 연결이 끊기는 등), Spring Batch는 스텝을 `UNKNOWN`으로 표시하고 사람이 확인하기 전에는 재시작하지 않습니다.

근거 소스:

- [`ChunkOrientedStep` (Spring Batch v6.0.6)](https://github.com/spring-projects/spring-batch/blob/v6.0.6/spring-batch-core/src/main/java/org/springframework/batch/core/step/item/ChunkOrientedStep.java)
- [`AbstractJobRepositoryFactoryBean` (Spring Batch v6.0.6)](https://github.com/spring-projects/spring-batch/blob/v6.0.6/spring-batch-core/src/main/java/org/springframework/batch/core/repository/support/AbstractJobRepositoryFactoryBean.java)
- [`DataSourceTransactionManager` (Spring Framework v7.0.10)](https://github.com/spring-projects/spring-framework/blob/v7.0.10/spring-jdbc/src/main/java/org/springframework/jdbc/datasource/DataSourceTransactionManager.java)

## AI 코딩 에이전트

- `AGENTS.md`에 이 프로젝트에서 지켜야 할 규칙을 적었습니다. 여러 에이전트가 함께 읽도록 영어로 썼습니다.
- Claude Code는 `CLAUDE.md`가 없으면 `AGENTS.md`를 읽습니다(v2.1.277 이상). Codex도 `AGENTS.md`를 읽습니다.
- `.claude/settings.json`이 Claude Code 훅을 등록합니다. 실패하면 종료 코드 2로 에이전트에게 알립니다.

| 훅 | 스크립트 | 하는 일 |
|---|---|---|
| 파일 수정 뒤 | `scripts/check-java.sh` | 메인 코드의 `@Autowired`, `@Inject`, `@Resource`, 테스트의 `@DisplayName` 누락을 검사하고 컴파일 |
| 작업 종료 전 | `scripts/run-tests.sh` | 전체 테스트 실행 |

- `check-java.sh`는 `jq`가 없으면 검사를 건너뜁니다. `run-tests.sh`는 `jq` 없이 항상 테스트를 실행합니다.
- `run-tests.sh`는 마지막으로 테스트가 통과한 뒤 코드가 바뀌었을 때만 테스트를 실행합니다. 커밋, 작업 트리 변경, `HEAD` 이동을 모두 변경으로 봅니다. git이 없으면 매번 실행합니다.
- 테스트가 실패하면 통과할 때까지 작업 종료를 막습니다. Claude Code의 연속 차단 상한은 Claude가 도구를 호출할 때마다 초기화됩니다. 그래서 JDK 설치나 의존성 다운로드처럼 코드 밖의 원인으로 계속 실패하면, 사람이 멈추거나 원인을 고쳐야 합니다.
- `check-java.sh`는 줄 단위 문자열 검사입니다. 주석 줄은 건너뛰지만, 클래스에 붙은 `@DisplayName`도 개수에 포함하는 등 한계가 있습니다. 정교한 규칙은 ArchUnit 테스트로 옮기세요.
- 다른 에이전트에서도 같은 스크립트를 훅으로 등록해 쓸 수 있습니다. 이 저장소에는 Claude Code 설정만 포함했습니다.

## 포함하지 않은 것

- HTTP로 잡을 실행하는 모듈
- 스킵, 재시도, 리스너 예시
- 스케줄링과 모니터링 구성

## 참고 자료

- [Spring Batch Reference Documentation](https://docs.spring.io/spring-batch/reference/)
- [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- 정상혁, 『AI 에이전트와 스프링 배치를 활용한 배치 엔지니어링』, 위키북스, 2026.
  - 모듈 구성(공통 배치 모듈, 잡 그룹 모듈, 실행 모듈), 잡 그룹 단위 `@Import` 구성, 빌드 설정, 두 가지 실행 방식(`CommandLineJobOperator`, `ApplicationRunner`)은 이 책 14장의 예제 구성을 따랐습니다.
  - `AGENTS.md`의 규칙 상당수는 이 책 14.5.2절과 16.2.2절의 AGENTS.md 예시를 이 템플릿에 맞게 영어로 옮기고 줄여 재구성했습니다.
  - 두 DB 분리에 따른 재시작 전략, 종료 코드 처리, 샘플 잡은 이 저장소에서 새로 설계했습니다.

## 라이선스

[MIT License](LICENSE)
