package com.likelion.mtm.infra.imagegen;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.product.entity.Currency;
import com.likelion.mtm.domain.product.entity.Dimensions;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.WearType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 착용 이미지 프롬프트에 생성 합성에 필요한 정보와 지시가 포함되는지 검증한다.
 */
class WornImagePromptAssemblerTest {

    private final WornImagePromptAssembler promptAssembler = new WornImagePromptAssembler();

    @Test
    @DisplayName("착용 이미지 프롬프트에 회원 신체 정보와 제품 실측 치수가 포함된다")
    void includeBodyInfoAndDimensions() {
        String prompt = promptAssembler.assemble(member(), product(WearType.CROSSBODY));

        assertThat(prompt)
                .contains("175.5 cm")
                .contains("68.0 kg")
                .contains("depth 4.25 inches")
                .contains("width 11.50 inches")
                .contains("height 8.75 inches")
                .contains("relative scale");
    }

    @ParameterizedTest(name = "{0} 착용 방식을 구체적인 위치 지시로 변환한다")
    @MethodSource("wearTypeInstructions")
    @DisplayName("각 착용 방식을 생성 모델이 이해할 수 있는 지시로 변환한다")
    void includeWearTypeInstruction(WearType wearType, String expectedInstruction) {
        String prompt = promptAssembler.assemble(member(), product(wearType));

        assertThat(prompt).contains(expectedInstruction);
    }

    @Test
    @DisplayName("두 입력 이미지의 역할과 인물 및 제품 보존 지시가 포함된다")
    void distinguishInputsAndPreserveSourceImages() {
        String prompt = promptAssembler.assemble(member(), product(WearType.IN_HAND));

        assertThat(prompt)
                .contains("first input image is the member's base image")
                .contains("second input image is the product cut")
                .contains("Preserve the identity, face, body shape")
                .contains("Keep the background, full-body framing, and overall posture")
                .contains("Preserve the product cut's design, shape, color, material appearance")
                .contains("Do not replace, redesign, or transform the product");
    }

    /**
     * 착용 방식별로 프롬프트에 포함되어야 할 핵심 위치 지시를 제공한다.
     */
    private static Stream<Arguments> wearTypeInstructions() {
        return Stream.of(
                Arguments.of(WearType.ONE_SHOULDER, "strap resting naturally on that shoulder"),
                Arguments.of(WearType.CROSSBODY, "strap running diagonally from one shoulder across the torso"),
                Arguments.of(WearType.IN_HAND, "naturally in one hand"),
                Arguments.of(WearType.WAIST, "securely around the waist"),
                Arguments.of(WearType.BESIDE, "upright on the floor beside the person")
        );
    }

    /**
     * 프롬프트 테스트에 사용할 신체 정보가 입력된 회원을 만든다.
     */
    private Member member() {
        Member member = Member.register("member@example.com", "password-hash");
        member.updateBodyInfo(new BigDecimal("175.5"), new BigDecimal("68.0"));
        return member;
    }

    /**
     * 프롬프트 테스트에 사용할 실측 치수와 착용 방식이 입력된 제품을 만든다.
     */
    private Product product(WearType wearType) {
        return Product.of(
                "MMHGATA01CO001",
                "MMHGATA01CO",
                "Test Product",
                "Black",
                new BigDecimal("1000000.00"),
                Currency.KRW,
                "Test product description",
                Dimensions.of(
                        new BigDecimal("4.25"),
                        new BigDecimal("11.50"),
                        new BigDecimal("8.75")
                ),
                wearType,
                "https://example.com/products/MMHGATA01CO001"
        );
    }
}
