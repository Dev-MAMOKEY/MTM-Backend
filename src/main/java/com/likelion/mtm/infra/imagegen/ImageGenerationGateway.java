package com.likelion.mtm.infra.imagegen;

/**
 * 이미지 생성 모델을 교체 가능하게 만드는 게이트웨이 인터페이스.
 * 도메인 서비스는 Gemini 같은 구체적인 공급자 대신 이 인터페이스에만 의존한다.
 */
public interface ImageGenerationGateway {

    /**
     * 프롬프트와 입력 이미지들을 사용해 새로운 이미지를 생성한다.
     *
     * @param request 이미지 생성 요청
     * @return 생성된 이미지 데이터
     */
    GeneratedImage generate(ImageGenerationRequest request);
}
