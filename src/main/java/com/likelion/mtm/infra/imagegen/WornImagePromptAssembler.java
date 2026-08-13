package com.likelion.mtm.infra.imagegen;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.product.entity.Dimensions;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.WearType;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 기준 이미지와 제품 컷으로 착용 이미지를 생성하기 위한 프롬프트를 조립한다.
 */
@Component
public class WornImagePromptAssembler {

    /**
     * 회원 신체 정보, 제품 실측 치수와 착용 방식을 포함한 착용 이미지 생성 프롬프트를 만든다.
     *
     * @param member 기준 이미지의 소유 회원
     * @param product 착용 이미지에 표현할 제품
     * @return 착용 이미지 생성 프롬프트
     */
    public String assemble(Member member, Product product) {
        Objects.requireNonNull(member, "회원은 필수입니다.");
        Objects.requireNonNull(product, "제품은 필수입니다.");
        Objects.requireNonNull(member.getHeightCm(), "회원 키는 필수입니다.");
        Objects.requireNonNull(member.getWeightKg(), "회원 몸무게는 필수입니다.");

        Dimensions dimensions = Objects.requireNonNull(
                product.getDimensions(),
                "제품 실측 치수는 필수입니다."
        );
        Objects.requireNonNull(dimensions.getDepthIn(), "제품 실측 깊이는 필수입니다.");
        Objects.requireNonNull(dimensions.getWidthIn(), "제품 실측 너비는 필수입니다.");
        Objects.requireNonNull(dimensions.getHeightIn(), "제품 실측 높이는 필수입니다.");

        String wearInstruction = wearInstruction(product.getWearType());

        return """
                Create a photorealistic worn image by generatively composing the provided inputs.
                The first input image is the member's base image, and the second input image is the product cut.
                Preserve the identity, face, body shape, and body proportions of the person in the base image.
                Keep the background, full-body framing, and overall posture of the base image as unchanged as possible.
                The member's height is %s cm and weight is %s kg. Use this body information as the reference for the product's relative scale and placement.
                The product's measured dimensions are depth %s inches, width %s inches, and height %s inches.
                Depict the product at a visually plausible relative scale to the person's body based on these measured dimensions and the member's body information.
                %s
                Place and wear the product naturally according to this instruction without unnecessarily changing the person's pose.
                Preserve the product cut's design, shape, color, material appearance, pattern, and distinctive details as faithfully as possible.
                Do not replace, redesign, or transform the product into a different product or an invented design.
                Produce one final worn image with no added text or watermark.
                """.formatted(
                member.getHeightCm().toPlainString(),
                member.getWeightKg().toPlainString(),
                dimensions.getDepthIn().toPlainString(),
                dimensions.getWidthIn().toPlainString(),
                dimensions.getHeightIn().toPlainString(),
                wearInstruction
        );
    }

    /**
     * 착용 방식 값을 생성 모델이 이해할 수 있는 구체적인 위치와 자세 지시로 변환한다.
     */
    private String wearInstruction(WearType wearType) {
        Objects.requireNonNull(wearType, "제품 착용 방식은 필수입니다.");

        return switch (wearType) {
            case ONE_SHOULDER ->
                    "Wear the product from one shoulder, with its strap resting naturally on that shoulder and the product hanging beside the torso.";
            case CROSSBODY ->
                    "Wear the product crossbody, with its strap running diagonally from one shoulder across the torso and the product resting naturally at the opposite hip.";
            case IN_HAND ->
                    "Place the product naturally in one hand, with the arm relaxed and the product hanging beside the corresponding leg.";
            case WAIST ->
                    "Wear the product securely around the waist, centered or slightly to one side at a natural waist-level position.";
            case BESIDE ->
                    "Stand the product upright on the floor beside the person, close to one leg, without attaching it to the body.";
        };
    }
}
