# MTM Backend

MCM 제품 가상 착용 서비스 — 백엔드

회원이 올린 전신 사진을 **기준 이미지**(정자세·흰 배경)로 한 번 변환해 두고, MCM 제품을 고를 때마다 그 위에 제품이 올라간 **착용 이미지**를 생성한다.

```
원본 사진  ──(AI 1회)──▶  기준 이미지  ──(AI, 제품마다)──▶  착용 이미지
회원 업로드              정자세·흰 배경                  (사진, SKU)별 저장·재사용
```

- **배포 서비스**: https://52.79.118.19
- **API 문서(Swagger)**: https://52.79.118.19/swagger-ui/index.html
- 프론트엔드: [Dev-MAMOKEY/MTM-frontend](https://github.com/Dev-MAMOKEY/MTM-frontend)
- PRD: [#2 PRD v2](https://github.com/Dev-MAMOKEY/MTM-Backend/issues/2)
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
| DB | MySQL (AWS RDS) | 8.x |
| 인증 | Spring Security + JWT (jjwt) | 0.13.0 |
| 이미지 저장소 | AWS SDK for Java v2 (S3) | BOM 2.51.2 |
| 이미지 생성 | Google Gen AI Java SDK (Gemini) | 1.65.0 |
| 이미지 생성(폴백) | OpenAI Java SDK | 4.50.0 |
| API 문서 | springdoc-openapi | 3.1.0 |
| 배포 | Docker · AWS EC2 | - |
| 테스트 | JUnit 5, Spring Boot Test, H2 | Boot 관리 |

---

## 구현 현황

| 슬라이스 | 이슈 | 내용 | 상태 |
|---|---|---|---|
| 1 | #3 | 제품 목록이 화면에 뜬다 | ✅ (#24로 구현) |
| 2 | #4 | 가입하고 로그인할 수 있다 | ✅ |
| 3 | #5 | 신체 정보를 입력하고 저장한다 | ✅ |
| 4 | #6 | 사진을 올리면 사진첩에 쌓인다 | ✅ |
| 5 | #7 | 올린 사진이 기준 이미지로 바뀐다 | ✅ |
| 6 | #8 | 제품을 고르면 착용 이미지가 만들어진다 | ✅ |
| 7 | #9 | 이미 본 제품은 즉시 다시 뜬다 | ✅ |
| 8 | #10 | 착용 이미지를 다시 만들 수 있다 | ✅ |
| 9 | #11 | 기준 이미지를 다시 만들면 착용 이미지가 정리된다 | ✅ |
| 10 | #12 | OpenAI로 갈아끼워 비교한다 | ✅ Gemini 최종 채택 |

**슬라이스 10개 전부 완료.** MCM 제품 **30 SKU**와 제품 컷 **231장**이 적재되어 있다.

이미지 생성은 **처음에 OpenAI로 구현했다.** 테스트 과정에서 응답 시간과 결과 품질에 문제가 있어 Gemini와 비교 테스트를 진행했고, 팀 회의를 거쳐 **Gemini를 최종 채택**했다. 자세한 경위는 [트러블슈팅](#트러블슈팅)에 정리했다.

### API 목록

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/auth/signup` | 회원가입 |
| `POST` | `/api/v1/auth/login` | 로그인 (액세스·리프레시 토큰 발급) |
| `POST` | `/api/v1/auth/reissue` | 액세스 토큰 재발급 |
| `GET` | `/api/v1/members/me` | 내 정보 조회 |
| `POST` | `/api/v1/members/me/body-info` | 신체 정보 저장 |
| `PATCH` | `/api/v1/members/me/body-info` | 신체 정보 부분 수정 |
| `GET` | `/api/v1/products` | 제품 목록 조회 (페이징) |
| `GET` | `/api/v1/products/{productId}` | 제품 상세 조회 |
| `POST` | `/api/v1/photos` | 원본 사진 업로드 |
| `GET` | `/api/v1/photos` | 사진첩 조회 |
| `DELETE` | `/api/v1/photos/{photoId}` | 사진 삭제 (기준·착용 이미지 연쇄 삭제) |
| `POST` | `/api/v1/photos/{photoId}/base-image` | 기준 이미지 생성 |
| `POST` | `/api/v1/photos/{photoId}/base-image/regenerate` | 기준 이미지 재생성 |
| `GET` | `/api/v1/base-images` | 내 기준 이미지 목록 조회 |
| `POST` | `/api/v1/base-images/{baseImageId}/worn-images` | 착용 이미지 생성 (같은 조합은 재사용) |
| `POST` | `/api/v1/base-images/{baseImageId}/worn-images/regenerate` | 착용 이미지 재생성 |
| `GET` | `/api/v1/base-images/{baseImageId}/worn-images` | 기준 이미지별 착용 이미지 목록 조회 |

---

## 트러블슈팅

### 1. 이미지 생성 공급자를 OpenAI에서 Gemini로 교체

이미지 생성은 처음에 **OpenAI(`gpt-image-2`)로 구현**했다. 동작은 했지만 테스트에서 두 가지가 걸렸다.

- **응답 시간** — 길게는 30~40초. 로딩바로 버티기 어렵고, 리버스 프록시를 붙이면 기본 타임아웃(nginx 60초)에도 걸릴 수 있는 값이다.
- **결과 품질** — 제품이 원본과 다르게 그려지는 빈도가 높았다.

그래서 Gemini와 비교 테스트를 진행했고, **응답 시간이 10초 이내로 짧고 결과도 더 안정적**이어서 팀 회의를 거쳐 Gemini를 최종 채택했다.

교체 비용이 낮았던 건 `ImageGenerationGateway` 인터페이스 뒤에 구현체를 둔 구조 덕분이다. **도메인 코드는 한 줄도 바뀌지 않았고, 환경 변수 하나로 전환된다.**

```
IMAGE_GENERATION_PROVIDER=GEMINI
```

Gemini 안에서 어떤 모델을 쓸지도 측정으로 정했다. `GeminiImageModelBenchmark`가 후보 3종(`gemini-3.1-flash-lite-image` / `gemini-3.1-flash-image` / `gemini-3-pro-image`)을 실제 호출해 모델별 지연시간과 결과 이미지를 비교한다. 과금이 발생하므로 CI에서는 제외하고 필요할 때만 수동으로 실행한다.

> OpenAI 구현체는 지우지 않고 폴백으로 남겨 두었다. 설정만 바꾸면 언제든 되돌릴 수 있다.

### 2. 같은 제품을 다시 고를 때마다 AI를 다시 호출

생성 결과가 매번 달라지는 특성(ADR 0001) 때문에 **(기준 이미지, 제품) 조합으로 저장**해 두고 재사용한다. 같은 조합을 다시 요청하면 AI를 호출하지 않고 저장된 결과를 바로 돌려준다.

**수십 초에서 1초 미만으로 줄었고**, AI 호출 비용도 그만큼 아꼈다.

### 3. 크롤링 데이터의 통화가 스키마와 맞지 않음

`product.currency`가 `ENUM('KRW')`인데 크롤링한 30건이 전부 USD였다. `ddl-auto: validate`라 적재가 아예 되지 않는 상황이었다.

환율로 환산하면 원본 가격 정보가 사라지므로 **`ENUM('KRW','USD')`로 확장하고 원본 통화를 그대로 저장**하는 쪽을 택했다. 표기 방식은 클라이언트가 정한다.

### 4. 사진을 삭제해도 저장소에 파일이 남음

외래 키가 `ON DELETE CASCADE`라 사진만 지워도 DB는 정리되지만, 행이 사라지는 순간 `storage_key`를 잃어 **S3 객체가 영구히 남는다.**

지우기 전에 저장소 키를 모으고 자식(착용 이미지 → 기준 이미지 → 원본 사진)부터 명시적으로 삭제한다. 저장소 삭제는 DB 삭제가 끝난 뒤 마지막에 수행한다 — 트랜잭션 안에서 지웠다가 롤백되면 DB는 되돌아가도 파일은 복구할 수 없기 때문이다.

### 5. 목록 조회의 N+1

제품 목록의 대표 이미지, 사진첩의 기준 이미지를 항목마다 조회하면 N+1이 발생한다. 두 곳 모두 **현재 페이지 분량을 한 번에 조회해 `Map`으로 묶어** 매칭하도록 처리했다.

### 남아 있는 과제

- **이미지 생성 실패가 로그에 남지 않는 경로가 있다.** SDK 예외는 로그가 남지만, 응답에 이미지가 없어 `parseGeneratedImage`에서 던지는 경우는 로그 없이 502만 나간다. 원인 추적이 어려워 개선이 필요하다.
- **`product_cut.is_worn_slot`이 전부 `false`다.** 크롤링 데이터에 모델 착용 컷을 판정할 근거가 없어 적재 시 지정하지 못했다.

---

## 프로젝트 구조

```
com.likelion.mtm
├── MtmApplication.java
│
├── global                          # 도메인 공통
│   ├── config                      # SecurityConfig, SwaggerConfig, JpaAuditingConfig,
│   │                               # S3Config, GeminiConfig, OpenAiConfig, ImageGenerationConfig
│   ├── common                      # BaseTimeEntity, PageResponseDTO<T>
│   ├── rsdata                      # RsData<T>
│   ├── exception                   # CustomException, ErrorCode, GlobalExceptionHandler
│   └── security                    # JwtProvider, JwtAuthenticationFilter,
│                                   # JwtAuthenticationEntryPoint, JwtAccessDeniedHandler
│
├── domain
│   ├── member                      # 회원 · 인증 · 신체 정보               [슬라이스 2, 3]
│   ├── product                     # 제품 · 실측 치수 파서 · 착용 방식 분류기 · 크롤링 적재기 [슬라이스 1]
│   ├── photo                       # 원본 사진(사진첩) · 기준 이미지        [슬라이스 4, 5, 9]
│   └── worn                        # 착용 이미지 · 재사용 · 재생성          [슬라이스 6, 7, 8]
│
└── infra                           # 교체 가능한 외부 연동 — PRD가 요구하는 경계
    ├── storage
    │   ├── ImageStorage            # 인터페이스
    │   ├── S3ImageStorage          # 구현체
    │   └── ImageData               # 이미지 바이너리 + MIME 타입
    └── imagegen
        ├── ImageGenerationGateway  # 인터페이스
        ├── GeminiImageGateway      # 주력
        ├── OpenAiImageGateway      # 폴백
        ├── BaseImagePromptAssembler   # 기준 이미지 프롬프트 조립기
        ├── WornImagePromptAssembler   # 착용 이미지 프롬프트 조립기
        └── ImageGenerationRequest / GeneratedImage / ImageInput / ImageGenerationProvider
```

각 `domain` 패키지 내부는 `controller / service / repository / entity / dto`로 나눈다.
테스트용 `FakeImageGateway`는 `src/test/java/com/likelion/mtm/infra/imagegen`에 있다.

---

## 실행 방법

### 1. 환경 변수

레포 루트에 `.env`를 만든다. **`.env`는 커밋하지 않는다** (`.gitignore` 확인).

```
DOCKER_IMAGE={본인계정}/mtm-backend:latest
DATABASE_URL=jdbc:mysql://{RDS엔드포인트}:3306/first_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DATABASE_USERNAME=
DATABASE_PASSWORD=
JWT_SECRET={32자 이상 랜덤 문자열}
JWT_ACCESS_EXP_MIN=60
JWT_REFRESH_EXP_DAY=30
IMAGE_GENERATION_PROVIDER=NONE
GEMINI_API_KEY=
OPENAI_API_KEY=
```

- `DATABASE_URL`은 **JDBC URL 전체**를 넣는다. 호스트만 넣으면 기동에 실패한다.
- `IMAGE_GENERATION_PROVIDER`는 `GEMINI` / `OPENAI` / `NONE` 중 하나다. `NONE`이면 이미지 생성 기능만 빠지고 회원·사진·제품 기능은 정상 기동한다. AI 키 없이 개발할 때 쓴다.
- **AWS 자격증명은 `.env`에 넣지 않는다.** 로컬은 `~/.aws/credentials`, 배포 서버는 EC2 IAM 역할로 주입된다.

### 2. 로컬 실행

```bash
./gradlew bootRun
```

Swagger — http://localhost:8080/swagger-ui/index.html

### 3. 배포

**로컬에서 이미지를 만들어 올린다**

```bash
docker build -t {본인계정}/mtm-backend:latest .
docker login
docker push {본인계정}/mtm-backend:latest
```

**서버에서 받아 실행한다**

서버 작업 디렉터리에 `docker-compose.yml`과 `.env`가 있어야 한다. `.env`는 깃에 없으므로 서버에서 직접 만든다.

```bash
docker compose pull
docker compose up -d
docker compose logs -f
```

- `restart: unless-stopped`라 서버가 재부팅돼도 컨테이너가 자동으로 올라온다.
- `.env`에 `DOCKER_IMAGE`가 없으면 compose가 기동을 거부한다.

**HTTPS**

프론트엔드가 HTTPS로 배포되어 브라우저가 혼합 콘텐츠를 차단하므로, 백엔드도 HTTPS로 서비스한다.

- 리버스 프록시가 **443**에서 TLS를 종료하고 컨테이너의 8080으로 전달한다. 80으로 들어온 요청은 HTTPS로 리다이렉트된다.
- 인증서는 Let's Encrypt를 쓰며, 도메인 없이 **IP 주소(`52.79.118.19`)로 발급**했다.
- 프록시가 TLS를 끊고 백엔드에는 HTTP로 넘기기 때문에, 스프링이 원래 프로토콜을 인식하도록 아래 설정이 필요하다. 없으면 `Secure` 쿠키나 리다이렉트 주소가 `http://`로 나간다.

```yaml
server:
  forward-headers-strategy: framework
```

- 외부 접속이 되려면 **EC2 보안 그룹과 서버 방화벽(`ufw`)에 80·443**이 열려 있어야 한다.

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
- **엔티티에 `@Setter`를 붙이지 않는다.** 의미 있는 메서드로 상태를 바꾼다 (`updateBodyInfo()`, `replaceStorageKey()` 등)
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
  // X — IntelliJ 실행 시 500
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

| 트랙 | 담당 | 담당한 작업 |
|---|---|---|
| A. 회원 · 제품 · 배포 | 김동환 | 슬라이스 2·3(#4·#5), 슬라이스 1(#3→#24), 제품 상세·목록 페이징(#34), 착용 방식 노출(#37), 사진 삭제(#38), Docker 배포 환경(#31) |
| B. 이미지 생성 인프라 | 최정훈 | 슬라이스 4·5·6(#6·#7·#8), `ImageStorage`·`ImageGenerationGateway` 추상화, Gemini·OpenAI 게이트웨이, AWS 인프라 |
| C. 이미지 파이프라인 | 유소영 | ERD·엔티티(#13), 크롤링 데이터 수집, 슬라이스 7·8·9(#9·#10·#11), 기준 이미지·착용 이미지 조회 API(#41·#43·#45) |

- A/B/C가 마주치는 접점은 `ImageGenerationGateway` / `ImageStorage` **인터페이스 시그니처**뿐이다. Phase 0에서 확정한 뒤 바꾸지 않는다.
- `#10`, `#11`은 같은 파일(`domain/worn`)을 건드리므로 동시에 진행하지 않고 순서대로 처리했다.