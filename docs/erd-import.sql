CREATE TABLE member (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 식별자',
    email         VARCHAR(255) NOT NULL COMMENT '로그인 이메일',
    password_hash VARCHAR(255) NOT NULL COMMENT '비밀번호 해시. 평문 저장 금지',
    height_cm     DECIMAL(4,1) NULL COMMENT '키(cm). 가입 시점에는 없고 신체 정보 입력에서 채운다',
    weight_kg     DECIMAL(4,1) NULL COMMENT '몸무게(kg). 프롬프트 조립기의 유일한 사용처',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) COMMENT '회원';

CREATE TABLE photo (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '원본 사진 식별자',
    member_id   BIGINT       NOT NULL COMMENT '소유 회원',
    storage_key VARCHAR(500) NOT NULL COMMENT 'ImageStorage 키',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_photo_storage_key (storage_key),
    KEY ix_photo_member (member_id, id),
    CONSTRAINT fk_photo_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT '원본 사진. 이것들이 모여 사진첩이 된다. 개수 제한 없음';

CREATE TABLE base_image (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기준 이미지 식별자',
    photo_id    BIGINT       NOT NULL COMMENT '원본 사진. 사진 하나당 기준 이미지 하나',
    storage_key VARCHAR(500) NOT NULL COMMENT 'ImageStorage 키',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_base_image_photo (photo_id),
    UNIQUE KEY uk_base_image_storage_key (storage_key),
    CONSTRAINT fk_base_image_photo FOREIGN KEY (photo_id) REFERENCES photo (id)
) COMMENT '기준 이미지. 정자세 흰 배경. 배경 제거는 하지 않는다';

CREATE TABLE product (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '제품 식별자',
    sku                 VARCHAR(50)   NOT NULL COMMENT '제품 식별자. 색상까지 구분된 단위',
    base_sku            VARCHAR(20)   NOT NULL COMMENT 'SKU 앞 11자. 사이즈 변형끼리 제품 컷이 같다',
    name                VARCHAR(255)  NOT NULL COMMENT '제품명',
    color               VARCHAR(100)  NULL COMMENT '색상',
    price               DECIMAL(10,2) NULL COMMENT '가격. 제품 목록 화면에 표시된다',
    currency            CHAR(3)       NULL COMMENT 'ISO 4217 통화 코드',
    description         TEXT          NULL COMMENT '제품 상세 원문. 실측 치수 파서의 입력',
    dimension_depth_in  DECIMAL(5,2)  NULL COMMENT '실측 깊이(inch)',
    dimension_width_in  DECIMAL(5,2)  NULL COMMENT '실측 너비(inch)',
    dimension_height_in DECIMAL(5,2)  NULL COMMENT '실측 높이(inch)',
    wear_type           VARCHAR(20)   NOT NULL COMMENT '착용 방식. ONE_SHOULDER CROSSBODY IN_HAND WAIST BESIDE. 백팩은 ONE_SHOULDER',
    detail_url          VARCHAR(500)  NULL COMMENT '제품 상세 페이지 URL',
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku (sku),
    KEY ix_product_base_sku (base_sku)
) COMMENT '제품. SKU 단위. 일회성 적재';

CREATE TABLE product_cut (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '제품 컷 식별자',
    product_id    BIGINT       NOT NULL COMMENT '소속 제품',
    storage_key   VARCHAR(500) NOT NULL COMMENT 'ImageStorage 키',
    slot_no       INT          NOT NULL COMMENT '슬롯 번호. SKU마다 연속이 아니다',
    is_front_slot BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '정면 슬롯 여부. Java 필드명은 frontSlot',
    is_worn_slot  BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '착용 슬롯 여부. Java 필드명은 wornSlot',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY ix_product_cut_front (product_id, is_front_slot),
    CONSTRAINT fk_product_cut_product FOREIGN KEY (product_id) REFERENCES product (id)
) COMMENT '제품 컷. MCM이 촬영한 제품 사진';

CREATE TABLE worn_image (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '착용 이미지 식별자',
    base_image_id  BIGINT       NOT NULL COMMENT '바탕이 된 기준 이미지',
    product_id     BIGINT       NOT NULL COMMENT '올라간 제품',
    storage_key    VARCHAR(500) NOT NULL COMMENT 'ImageStorage 키. 다시 만들기는 이 값을 교체한다',
    generator      VARCHAR(20)  NOT NULL COMMENT '생성 게이트웨이. GEMINI OPENAI FAKE',
    product_cut_id BIGINT       NULL COMMENT '프롬프트에 넣은 제품 컷',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_worn_image_storage_key (storage_key),
    KEY ix_worn_image_product (product_id),
    CONSTRAINT fk_worn_image_base_image FOREIGN KEY (base_image_id) REFERENCES base_image (id),
    CONSTRAINT fk_worn_image_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_worn_image_product_cut FOREIGN KEY (product_cut_id) REFERENCES product_cut (id)
) COMMENT '착용 이미지. 생성 결과가 매번 다르므로 저장은 필수';

CREATE UNIQUE INDEX uk_product_cut_slot ON product_cut (product_id, slot_no);

CREATE UNIQUE INDEX uk_worn_image_base_product ON worn_image (base_image_id, product_id);
