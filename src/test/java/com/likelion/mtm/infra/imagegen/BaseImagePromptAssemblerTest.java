package com.likelion.mtm.infra.imagegen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BaseImagePromptAssemblerTest {

    private final BaseImagePromptAssembler promptAssembler = new BaseImagePromptAssembler();

    @Test
    @DisplayName("기준 이미지 프롬프트에 신체 정보와 정자세 및 흰 배경 조건이 포함된다")
    void assembleBaseImagePrompt() {
        // when
        String prompt = promptAssembler.assemble(
                new BigDecimal("175.5"),
                new BigDecimal("68.0")
        );

        // then
        assertThat(prompt)
                .contains("175.5 cm")
                .contains("68.0 kg")
                .contains("front-facing posture")
                .contains("entire body visible")
                .contains("pure white background")
                .contains("Do not remove the background")
                .contains("do not create transparency");
    }
}
