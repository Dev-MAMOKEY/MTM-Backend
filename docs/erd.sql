-- =============================================================================
-- =============================================================================
-- 대상 DBMS : MySQL 8.0+ (CHECK 제약, DATETIME(6) 사용)
-- 문자셋    : utf8mb4
--
-- 관계 한눈에
--   member      → photo        1 : N   회원 한 명이 사진첩에 여러 장
--   photo       → base_image   1 : 1   사진 하나당 기준 이미지 하나
--   base_image  → worn_image   1 : N   기준 이미지 하나 위에 제품별로 여러 장
--   product     → worn_image   1 : N   제품 하나가 여러 회원의 사진에 올라감
--   product     → product_cut  1 : N   제품 하나에 촬영 컷 여러 장
--   product_cut → worn_image   1 : N   어떤 컷으로 그렸는지 (선택)
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS worn_image;
DROP TABLE IF EXISTS product_cut;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS base_image;
DROP TABLE IF EXISTS photo;
DROP TABLE IF EXISTS member;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- member — 회원
-- 이메일·비밀번호로 가입한 사용자. 신체 정보(키·몸무게)를 함께 들고 있다.
-- -----------------------------------------------------------------------------
CREATE TABLE member (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '회원 식별자',
    email         VARCHAR(255)    NOT NULL                COMMENT '이메일 — 로그인 아이디. 중복 가입 불가',
    password_hash VARCHAR(255)    NOT NULL                COMMENT '비밀번호 해시 — 평문으로 저장하지 않는다',
    height_cm     DECIMAL(4,1)    NULL                    COMMENT '키 (cm) — 가입 시점엔 비어 있고 신체 정보 입력에서 채운다',
    weight_kg     DECIMAL(4,1)    NULL                    COMMENT '몸무게 (kg) — 프롬프트 조립기가 쓰는 유일한 곳',
    created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)                          COMMENT '생성 시각',
    updated_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원';

-- -----------------------------------------------------------------------------
-- photo — 원본 사진
-- 회원이 업로드한 전신 사진. 아무 처리도 하지 않은 상태.
-- 한 회원의 사진들이 모여 사진첩이 된다. 개수 제한은 없다.
-- -----------------------------------------------------------------------------
CREATE TABLE photo (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '원본 사진 식별자',
    member_id    BIGINT       NOT NULL                COMMENT '소유 회원 → member.id. 사진첩은 본인 것만 조회된다',
    storage_key  VARCHAR(500) NOT NULL                COMMENT '저장소 키 — ImageStorage(로컬/S3)에서 파일을 찾는 키',
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)                          COMMENT '업로드 시각 — 사진첩 정렬 기준',
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (id),
    KEY idx_photo_member_id (member_id),
    CONSTRAINT fk_photo_member
        FOREIGN KEY (member_id) REFERENCES member (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='원본 사진';

-- -----------------------------------------------------------------------------
-- base_image — 기준 이미지
-- 원본 사진을 정자세·흰 배경으로 변환한 것. 한 원본 사진당 하나만 만들어 재사용한다.
-- 배경 제거는 하지 않는다 — 흰옷·밝은 피부가 함께 지워져 인물에 구멍이 생기기 때문.
-- -----------------------------------------------------------------------------
CREATE TABLE base_image (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기준 이미지 식별자',
    photo_id     BIGINT       NOT NULL                COMMENT '원본 사진 → photo.id. 1:1 — 사진 하나당 기준 이미지 하나',
    storage_key  VARCHAR(500) NOT NULL                COMMENT '저장소 키 — 다시 만들면 이 값이 교체된다',
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)                          COMMENT '생성 시각',
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각 — 다시 만든 시점',
    PRIMARY KEY (id),
    UNIQUE KEY uk_base_image_photo_id (photo_id),
    CONSTRAINT fk_base_image_photo
        FOREIGN KEY (photo_id) REFERENCES photo (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기준 이미지';

-- -----------------------------------------------------------------------------
-- product — 제품
-- MCM 제품. SKU 단위라 같은 모델의 다른 색은 다른 행이다. 크롤링 결과를 일회성으로 적재한다.
--
-- wear_type 값 — 착용 방식 분류기가 모든 SKU에 부여한다.
--   ONE_SHOULDER : 한쪽 어깨 (백팩도 여기 포함 — ADR 0001)
--   CROSSBODY    : 크로스백
--   IN_HAND      : 손에 듦
--   WAIST        : 허리
--   BESIDE       : 옆에 세움
-- -----------------------------------------------------------------------------
CREATE TABLE product (
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '제품 식별자',
    sku                   VARCHAR(50)    NOT NULL                COMMENT 'SKU — MMHGATA01CO001 형태. 색상까지 구분된 단위',
    base_sku              VARCHAR(20)    NOT NULL                COMMENT '기본 SKU — SKU 앞 11자. 사이즈 변형끼리 제품 컷이 동일하므로 이미지 매칭에 쓴다',
    name                  VARCHAR(255)   NOT NULL                COMMENT '제품명 — 목록 화면에 표시',
    color                 VARCHAR(100)   NULL                    COMMENT '색상',
    price                 DECIMAL(10,2)  NOT NULL                COMMENT '가격 — 목록 화면에 표시',
    currency              ENUM('KRW')    NOT NULL DEFAULT 'KRW'  COMMENT '통화 — ISO 4217. 현재 KRW만 취급 (해외 크롤링 추가 시 값 확장)',
    description           TEXT           NULL                    COMMENT '제품 상세 원문 — Approximately D x W x H inches, 실측 치수 파서의 입력',
    dimension_depth_in    DECIMAL(5,2)   NULL                    COMMENT '실측 깊이 (인치) — 원문이 인치라 그대로 저장',
    dimension_width_in    DECIMAL(5,2)   NULL                    COMMENT '실측 너비 (인치)',
    dimension_height_in   DECIMAL(5,2)   NULL                    COMMENT '실측 높이 (인치)',
    wear_type             ENUM('ONE_SHOULDER', 'CROSSBODY', 'IN_HAND', 'WAIST', 'BESIDE')
                                          NOT NULL                COMMENT '착용 방식 — 몸에 어떻게 지니는가. 가방 타입이 아니다',
    detail_url            VARCHAR(500)   NULL                    COMMENT '상세 페이지 URL',
    created_at            DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6)                          COMMENT '생성 시각',
    updated_at            DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku (sku),
    KEY idx_product_base_sku (base_sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='제품';

-- -----------------------------------------------------------------------------
-- product_cut — 제품 컷
-- MCM이 촬영한 제품 사진. SKU 하나에 여러 장이 딸린다.
-- (product_id, slot_no)가 UNIQUE. 다만 "SKU당 정면 슬롯은 1장"은 DB로 못 건다 —
-- MySQL에 부분 UNIQUE 인덱스가 없어서 적재 스크립트가 보장해야 한다.
-- -----------------------------------------------------------------------------
CREATE TABLE product_cut (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '제품 컷 식별자',
    product_id     BIGINT       NOT NULL                COMMENT '소속 제품 → product.id',
    storage_key    VARCHAR(500) NOT NULL                COMMENT '저장소 키',
    slot_no        INT          NOT NULL                COMMENT '슬롯 번호 — SKU마다 연속이 아니다 (예: 01, 04, 05, 09만 있는 제품도 있다)',
    is_front_slot  BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '정면 슬롯 여부 — 제품을 정면에서 찍은 컷. 목록의 대표 이미지로 쓴다',
    is_worn_slot   BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '착용 슬롯 여부 — MCM 모델이 실제 착용한 컷',
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)                          COMMENT '생성 시각',
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_cut_product_slot (product_id, slot_no),
    CONSTRAINT fk_product_cut_product
        FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='제품 컷';

-- -----------------------------------------------------------------------------
-- worn_image — 착용 이미지
-- 기준 이미지에 제품이 올라간 최종 결과물. 회원이 화면에서 보는 것.
-- (base_image_id, product_id)가 UNIQUE — "이미 본 제품은 즉시 다시 뜬다"의 실체.
-- 같은 조합이 이미 있으면 새로 생성하지 않고 저장된 걸 돌려준다.
-- -----------------------------------------------------------------------------
CREATE TABLE worn_image (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '착용 이미지 식별자',
    base_image_id   BIGINT       NOT NULL                COMMENT '바탕 기준 이미지 → base_image.id. 기준 이미지를 다시 만들면 함께 삭제된다',
    product_id      BIGINT       NOT NULL                COMMENT '올라간 제품 → product.id',
    storage_key     VARCHAR(500) NOT NULL                COMMENT '저장소 키 — 다시 만들기는 새 행이 아니라 이 값을 교체한다',
    generator       ENUM('GEMINI', 'OPENAI', 'FAKE')
                                  NOT NULL                COMMENT '생성 게이트웨이 — 두 AI 결과를 비교하려면 필요',
    product_cut_id  BIGINT       NULL                    COMMENT '사용한 제품 컷 → product_cut.id. 어떤 컷으로 그렸는지 추적용 (선택)',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)                          COMMENT '생성 시각',
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각 — 다시 만든 시점',
    PRIMARY KEY (id),
    UNIQUE KEY uk_worn_image_base_product (base_image_id, product_id),
    KEY idx_worn_image_product_id (product_id),
    KEY idx_worn_image_product_cut_id (product_cut_id),
    CONSTRAINT fk_worn_image_base_image
        FOREIGN KEY (base_image_id) REFERENCES base_image (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_worn_image_product
        FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_worn_image_product_cut
        FOREIGN KEY (product_cut_id) REFERENCES product_cut (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='착용 이미지';
