package com.likelion.mtm.domain.product.service;

import com.likelion.mtm.domain.product.entity.Dimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실측 치수 파서 테스트. 입력 문자열은 실제 크롤링 데이터에서 가져온 것이다.
 */
class DimensionParserTest {

    private final DimensionParser parser = new DimensionParser();

    @Test
    @DisplayName("details 안의 치수 문장에서 깊이·너비·높이를 뽑는다")
    void parsesDimensionLine() {
        List<String> details = List.of(
                "Adjustable fabric webbing strap",
                "Approximately 3.5 x 8.3 x 14.4 inches",
                "Made in Italy"
        );

        Optional<Dimensions> result = parser.parse(details);

        assertThat(result).isPresent();
        assertThat(result.get().getDepthIn()).isEqualByComparingTo(new BigDecimal("3.5"));
        assertThat(result.get().getWidthIn()).isEqualByComparingTo(new BigDecimal("8.3"));
        assertThat(result.get().getHeightIn()).isEqualByComparingTo(new BigDecimal("14.4"));
    }

    @Test
    @DisplayName("정수부만 두 자리인 값도 파싱한다")
    void parsesTwoDigitValues() {
        Optional<Dimensions> result =
                parser.parse(List.of("Approximately 6.1 x 13.0 x 16.1 inches"));

        assertThat(result).isPresent();
        assertThat(result.get().getWidthIn()).isEqualByComparingTo(new BigDecimal("13.0"));
    }

    @Test
    @DisplayName("치수 문장이 없으면 비어 있는 결과를 준다")
    void returnsEmptyWhenNoDimensionLine() {
        Optional<Dimensions> result =
                parser.parse(List.of("Interior pocket", "Made in Italy"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("details가 null이거나 비어 있어도 예외 없이 비어 있는 결과를 준다")
    void returnsEmptyWhenDetailsMissing() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse(List.of())).isEmpty();
    }
}
