package com.likelion.mtm.domain.worn.entity;

/**
 * 생성 게이트웨이 — 착용 이미지를 만든 AI. 두 AI 결과를 비교하려면 필요하다.
 * infra.imagegen의 ImageGenerationGateway 구현체(GeminiImageGateway / OpenAiImageGateway /
 * FakeImageGateway)와 1:1로 대응한다.
 */
public enum Generator {
    GEMINI,
    OPENAI,
    FAKE
}