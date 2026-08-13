package com.likelion.mtm.domain.product.entity;

/**
 * 통화 — ISO 4217.
 * 크롤링 원본이 미국 사이트라 USD가 들어온다. 환율로 환산하지 않고 원본 통화 그대로 저장한다.
 */
public enum Currency {
    KRW,
    USD
}
