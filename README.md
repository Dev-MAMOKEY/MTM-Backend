# MTM Backend

MCM 제품 가상 착용 서비스 — 백엔드

회원이 올린 전신 사진을 **기준 이미지**(정자세·흰 배경)로 한 번 변환해 두고, MCM 제품을 고를 때마다 그 위에 제품이 올라간 **착용 이미지**를 생성한다.

```
원본 사진  ──(AI 1회)──▶  기준 이미지  ──(AI, 제품마다)──▶  착용 이미지
회원 업로드              정자세·흰 배경                  (사진, SKU)별 저장·재사용
```

- 프론트엔드: [Dev-MAMOKEY/MTM-frontend](https://github.com/Dev-MAMOKEY/MTM-frontend)
- PRD: [#2 PRD v2](https://github.com/Dev-MAMOKEY/MTM-backend/issues/2)
- 용어집: [`CONTEXT.md`](./CONTEXT.md)
- 설계 결정: [`docs/adr/0001-generative-composition.md`](./docs/adr/0001-generative-composition.md)

---

## 기술 스택

| 구분 | 기술 | 버전 |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 4.1.0 |
| Build | Gradle (Groovy) | - |
| ORM | Spring Data JPA | Boot 관리 |
| DB | MySQL | 8.x |
| 인증 | Spring Security + JWT (jjwt) | 0.13.0 |
| 이미지 저장소 | AWS SDK for Java v2 (S3) / 로컬 파일 | BOM 2.51.2 |
| 이미지 생성 | Google Gen AI Java SDK (Gemini) | 1.65.0 |
| 이미지 생성(폴백) | OpenAI Java SDK | 4.50.0 *(조건부)* |
| API 문서 | springdoc-openapi | 3.1.0 |
| 테스트 | JUnit 5, Spring Boot Test, H2 | Boot 관리 |

---

## 프로젝트 구조

```
com.likelion.mtm
├── MtmApplication.java
│
├── global                          # 도메인 공통
│   ├── config                      # SecurityConfig, SwaggerConfig, JpaAuditingConfig
│   ├── common                      # RsData<T>, PageResponseDTO<T>, BaseTimeEntity
│   ├── exception                   # CustomException, ErrorCode, GlobalExceptionHandler
│   └── security                    # JwtProvider, JwtAuthenticationFilter, JwtAuthenticationEntryPoint
│
├── domain
│   ├── member                      # 회원 · 인증 · 신체 정보          [슬라이스 2, 3]
│   ├── product                     # 제품 · 실측 치수 파서 · 착용 방식 분류기 [슬라이스 1]
│   ├── photo                       # 원본 사진(사진첩) · 기준 이미지    [슬라이스 4, 5, 9]
│   └── worn                        # 착용 이미지 · 재사용 · 재생성      [슬라이스 6, 7, 8]
│
└── infra                           # 교체 가능한 외부 연동 — PRD가 요구하는 경계
    ├── storage
    │   ├── ImageStorage            # 인터페이스
    │   ├── LocalImageStorage       # 개발용
    │   └── S3ImageStorage          # 배포용
    └── imagegen
        ├── ImageGenerationGateway  # 인터페이스
        ├── GeminiImageGateway      # 주력
        ├── OpenAiImageGateway      # 폴백 (슬라이스 10, 조건부)
        ├── FakeImageGateway        # 테스트용
        └── PromptAssembler         # 프롬프트 조립기
```

각 `domain` 패키지 내부는 `controller / service / repository / entity / dto`로 나눈다.

---

## 실행 방법

### 1. 환경 변수

레포 루트에 `.env`를 만든다. **`.env`는 커밋하지 않는다** (`.gitignore` 확인).

---

## 팀 컨벤션

### 브랜치 전략

| 브랜치 | 용도 |
|---|---|
| `main` | 배포 |
| `develop` | 통합. **PR은 여기로 보낸다** |
| `{라벨}/#{이슈번호}` | 작업 브랜치. 예) `Feat/#3` |

- **이슈 하나당 브랜치 하나.**
- 작업 브랜치는 `develop`에서 따고, PR도 `develop`으로 보낸다.
- 이슈와 브랜치는 **담당자가 직접 만든다.**

### 커밋 메시지

```
[{라벨}/#{이슈번호}] {라벨}: {한글 설명}
```

예시:

```
[Feat/#3] Feat: 실측 치수 파서 구현
[Test/#3] Test: 착용 방식 분류기 테스트 추가
[Fix/#7] Fix: 기준 이미지 저장 시 storage key 중복 문제 수정
```

| 라벨 | 의미 |
|---|---|
| `Feat` | 새로운 기능 |
| `Fix` | 버그 수정 |
| `Refactor` | 기능 변화 없는 구조 개선 |
| `Design` | UI/응답 형태 변경 |
| `Chore` | 빌드·의존성 |
| `Setting` | 환경 설정 |
| `Comment` | 주석·로그 |
| `Docs` | 문서 |
| `Test` | 테스트 |

- **하나의 기능이 끝날 때마다 커밋한다.** 의미 단위로 잘게 쪼개되, 각 커밋은 빌드가 되어야 한다.

### 이슈 양식

```markdown
제목: [{라벨}] {번호} {목표}

## 목적

## 범위
### 포함:
### 제외:

## 작업 내용
- [ ]

## 완료 조건
- [ ]
```

> 슬라이스 이슈(#3~#12)는 PM이 이미 작성해 두었다. 새로 만드는 것은 이 양식을 따른다.

### PR 양식

```markdown
제목: [{라벨}/{Issue 번호}] 작업 요약

## 연관 Issue
- closes #{Issue 번호}

## 요약
이 PR에서 변경한 내용을 간단히 설명합니다.

## 변경 사항
- 변경 사항 1
- 변경 사항 2

## 범위
### 포함:
- 예시 1
### 제외:
- 예시 1

## 스크린샷 / 미리 보기
- before/after 이미지 또는 캡처 첨부
```

- **임의 섹션(API 명세, 리뷰 포인트 등)을 추가하지 않는다.** 필요하면 요약·변경 사항 안에 녹인다.
- 머지 전 **팀원 1명 이상 리뷰**를 받는다.

---

## 코드 컨벤션

### 계층

```
Controller  요청/응답 변환만. 비즈니스 로직 금지
Service     로직 · 트랜잭션
Repository  DB 접근
```

### 응답과 예외

- 모든 응답은 `RsData<T>`로 감싼다.
- 예외는 `CustomException` + `ErrorCode`를 던지고 `GlobalExceptionHandler`가 처리한다.
- **내부 예외 메시지를 그대로 응답에 노출하지 않는다.**
- 페이징 응답은 `PageResponseDTO<T>`로 감싼다. **Spring의 `Page`를 직접 노출하지 않는다.**

### 엔티티와 DTO

- 엔티티 → DTO 변환은 DTO의 정적 메서드 `from()`
- DTO → 엔티티 변환은 `toEntity()`
- **엔티티에 `@Setter`를 붙이지 않는다.** 의미 있는 메서드로 상태를 바꾼다 (`updateBodyInfo()`, `replaceBaseImage()` 등)
- 공통 시간 컬럼은 `BaseTimeEntity` 상속

### 트랜잭션

- 조회 전용 서비스 메서드는 `@Transactional(readOnly = true)`
- LAZY 연관을 컨트롤러에서 건드리면 500이 난다. **DTO 변환은 서비스 트랜잭션 안에서 끝낸다.**

### 주석과 문서

- **모든 코드에 한글 주석을 단다.** 각 클래스·메서드가 무슨 역할인지 읽고 바로 알 수 있게.
- **새 엔드포인트에는 Swagger 어노테이션을 전부 붙인다** (`@Tag`, `@Operation`, `@ApiResponses`). 나중에 몰아서 하면 안 붙는다.

### 파라미터

- `@PathVariable`, `@RequestParam`에 **이름을 반드시 명시한다.**
  ```java
  // O
  @PathVariable("id") Long id
  // X — IntelliJ 실행 시 500 (아래 트러블슈팅 1번)
  @PathVariable Long id
  ```

### 용어

`CONTEXT.md`의 용어를 따른다. 변수명·클래스명·주석·커밋 메시지 전부 해당된다.

| 쓸 것 | 쓰지 말 것 |
|---|---|
| 원본 사진 `Photo` | 업로드 이미지, 유저 이미지 |
| 기준 이미지 `BaseImage` | 정자세 이미지, 베이스 이미지, 누끼 이미지 |
| 착용 이미지 `WornImage` | 결과 이미지, 합성 이미지, 룩 |
| 제품 컷 `ProductCut` | 상품 이미지 |
| 실측 치수 `Dimensions` | 사이즈 |
| 착용 방식 `WearType` | 가방 타입, 카테고리 |
| 사진첩 | 갤러리, 앨범 |

> 특히 **생성 합성**과 **오려붙이기**를 "합성"으로 뭉뚱그리지 않는다. 어느 쪽인지 알 수 없어진다.

---

## 작업 분배

| 트랙 | 담당  | 슬라이스 |
|---|-----|---|
| A. 제품 & AI | 김동환 | #3 → #7(Gemini 구현체) → #8(프롬프트 조립기) → #12 |
| B. 회원 & 저장소 & 배포 | 최정훈 | #4 → #5 → #6 + 스토리지 추상화 + AWS 배포 |
| C. 이미지 파이프라인 | 유소영 | 뼈대 → #7(상위) → #8(서비스) → #9 → #10 → #11 |

### 진행 순서

```
Phase 0  스캐폴딩 · ERD 확정 · 인터페이스 2개 합의 · API 스펙 프론트 공유 · 키 발급
Phase 1  A:#3          B:#4          C:Fake 구현체 + 도메인 골격(테스트 먼저)
Phase 2  A:프롬프트 스파이크  B:#5 → #6   C:#7 상위 + #9 재사용 로직
Phase 3  실제 Gemini 연결 → #7 → #8 → #9 완료 → ⚠️ ADR 0001 재평가
Phase 4  C:#10, #11    A:#12(조건부)   B:배포
Phase 5  프론트 연동 마무리 · 데모 리허설
```