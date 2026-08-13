package com.likelion.mtm.domain.product.entity;

/**
 * 착용 방식 — 제품을 몸에 어떻게 지니는가. 가방 타입이 아니다.
 * 착용 방식 분류기가 크롤링된 모든 SKU에 부여한다.
 * 백팩도 ONE_SHOULDER로 분류한다 — 정면 정자세에서 등 뒤 백팩은 몸에 가려 보이지 않기 때문(ADR 0001).
 * 같은 토트라도 손에 들거나 어깨에 걸 수 있으므로, 가방 타입과는 다른 축이다.
 */
public enum WearType {

    /** 한쪽 어깨 (백팩 포함) */
    ONE_SHOULDER,

    /** 크로스백 */
    CROSSBODY,

    /** 손에 듦 */
    IN_HAND,

    /** 허리 */
    WAIST,

    /** 옆에 세움 */
    BESIDE
}