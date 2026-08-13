package com.likelion.mtm.domain.product.service;

import com.likelion.mtm.domain.product.entity.WearType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 착용 방식 분류기 테스트. SKU는 실제 크롤링 데이터의 값이다.
 */
class WearTypeClassifierTest {

    private final WearTypeClassifier classifier = new WearTypeClassifier();

    @Test
    @DisplayName("매핑표에 있는 SKU는 표의 값을 그대로 쓴다")
    void usesSkuMappingFirst() {
        assertThat(classifier.classify("MMLGATA02BK001", "Aren Sling Bag in Embossed Monogram Leather"))
                .isEqualTo(WearType.CROSSBODY);
        assertThat(classifier.classify("MMZGSFI01BK001", "Fursten Belt Bag in Monogram Nylon"))
                .isEqualTo(WearType.WAIST);
        assertThat(classifier.classify("MMVGSTT04BK001", "Ottomar Cabin Trolley in Visetos"))
                .isEqualTo(WearType.BESIDE);
    }

    @Test
    @DisplayName("백팩은 등 뒤가 보이지 않으므로 한쪽 어깨로 분류한다 (ADR 0001)")
    void classifiesBackpackAsOneShoulder() {
        assertThat(classifier.classify("MMKEAVE12BK001", "Stark Side Studs Backpack in Visetos"))
                .isEqualTo(WearType.ONE_SHOULDER);
    }

    @Test
    @DisplayName("매핑표에 없는 SKU는 제품명 키워드로 분류한다")
    void fallsBackToNameKeyword() {
        assertThat(classifier.classify("UNKNOWN0000001", "Some Crossbody in Visetos"))
                .isEqualTo(WearType.CROSSBODY);
        assertThat(classifier.classify("UNKNOWN0000002", "Some Belt Bag in Nylon"))
                .isEqualTo(WearType.WAIST);
        assertThat(classifier.classify("UNKNOWN0000003", "Some Clutch in Leather"))
                .isEqualTo(WearType.IN_HAND);
    }

    @Test
    @DisplayName("키워드에도 걸리지 않으면 기본값으로 분류해 적재가 멈추지 않게 한다")
    void fallsBackToDefault() {
        assertThat(classifier.classify("UNKNOWN0000004", "Mystery Bag"))
                .isEqualTo(WearType.ONE_SHOULDER);
        assertThat(classifier.classify("UNKNOWN0000005", null))
                .isEqualTo(WearType.ONE_SHOULDER);
    }
}
