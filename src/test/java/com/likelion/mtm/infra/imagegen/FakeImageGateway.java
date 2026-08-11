package com.likelion.mtm.infra.imagegen;

/**
 * 테스트에서 외부 API 호출 없이 고정 이미지를 반환하는 이미지 생성 게이트웨이.
 */
public class FakeImageGateway implements ImageGenerationGateway {

    private final GeneratedImage generatedImage;
    private ImageGenerationRequest lastRequest;
    private int callCount;

    /**
     * 기본 테스트 이미지로 Fake 게이트웨이를 만든다.
     */
    public FakeImageGateway() {
        this(new GeneratedImage("fake-image".getBytes(), "image/png"));
    }

    /**
     * 테스트가 지정한 결과 이미지로 Fake 게이트웨이를 만든다.
     */
    public FakeImageGateway(GeneratedImage generatedImage) {
        this.generatedImage = generatedImage;
    }

    /**
     * 호출 요청을 기록하고 고정된 생성 이미지를 반환한다.
     */
    @Override
    public GeneratedImage generate(ImageGenerationRequest request) {
        this.lastRequest = request;
        this.callCount++;
        return generatedImage;
    }

    /** 마지막으로 전달받은 생성 요청을 반환한다. */
    public ImageGenerationRequest getLastRequest() {
        return lastRequest;
    }

    /** 호출 횟수를 반환한다. */
    public int getCallCount() {
        return callCount;
    }
}
