package com.likelion.mtm.domain.product.service;

import com.likelion.mtm.domain.product.entity.WearType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * 착용 방식 분류기 — SKU마다 제품을 몸에 어떻게 지니는지를 정한다. 가방 타입이 아니다.
 *
 * 제품이 30개뿐이라 SKU 매핑표를 우선으로 두고, 표에 없으면 제품명 키워드로 폴백한다.
 * 착용 이미지 품질이 이 분류의 정확도에 직결되므로, 자동 규칙보다 사람이 검토한 표를 앞에 둔다.
 */
@Component
public class WearTypeClassifier {

    /**
     * 크롤링 30개 SKU 매핑.
     * 백팩을 ONE_SHOULDER로 두는 것은 ADR 0001 — 정면 정자세에서 등 뒤 백팩은 몸에 가려 보이지 않는다.
     */
    private static final Map<String, WearType> SKU_MAPPING = Map.ofEntries(
            // 백팩 — 등 뒤가 보이지 않으므로 한쪽 어깨에 건 모습으로 그린다
            Map.entry("MMKEAVE12BK001", WearType.ONE_SHOULDER), // Stark Side Studs Backpack
            Map.entry("MWKFATA01CO001", WearType.ONE_SHOULDER), // Aren Drawstring Backpack

            // 슬링 · 크로스백
            Map.entry("MMLGATA02BK001", WearType.CROSSBODY),    // Aren Sling Bag
            Map.entry("MMLGATA03CO001", WearType.CROSSBODY),    // Aren Sling Bag
            Map.entry("MMLGATA05K8001", WearType.CROSSBODY),    // Aren Sling
            Map.entry("MMRGATA04CO001", WearType.CROSSBODY),    // Aren Crossbody
            Map.entry("MMRGATA05BK001", WearType.CROSSBODY),    // Aren Crossbody
            Map.entry("MWEGSXT01CO001", WearType.CROSSBODY),    // Tracy Satchel — 작아서 크로스로 멘다
            Map.entry("MWRGAOB03BK001", WearType.CROSSBODY),    // Pina Tambourine Bag

            // 토트 · 쇼퍼 · 호보 · 숄더 — 어깨에 건다
            Map.entry("MMTGATA01BK001", WearType.ONE_SHOULDER), // Aren Nova Tote
            Map.entry("MMTGATA03BK001", WearType.ONE_SHOULDER), // Aren Nova Tote
            Map.entry("MWPGSLR02BK001", WearType.ONE_SHOULDER), // New Liz Shopper
            Map.entry("MWHGATA014B001", WearType.ONE_SHOULDER), // Aren Duo Hobo
            Map.entry("MWHGATA024B001", WearType.ONE_SHOULDER), // Aren Hobo
            Map.entry("MWRGAAK01BK001", WearType.ONE_SHOULDER), // Diamond Shoulder Bag
            Map.entry("MWDGADU02BK001", WearType.ONE_SHOULDER), // Dessau Drawstring Bag
            Map.entry("MWDGSDU01BK001", WearType.ONE_SHOULDER), // Dessau Drawstring Bag

            // 손에 듦 — 위켄더 · 가먼트 · 보스턴 · 클러치 · 배니티
            Map.entry("MMVGATT01BK001", WearType.IN_HAND),      // Ottomar Weekender Bag
            Map.entry("MMVGATT04BK001", WearType.IN_HAND),      // Ottomar Weekender
            Map.entry("MMVGATT08CO001", WearType.IN_HAND),      // MCM x We The Best Ottomar Weekender
            Map.entry("MMVGSTT03BK001", WearType.IN_HAND),      // Ottomar Garment Bag
            Map.entry("MWBFAEA03BK001", WearType.IN_HAND),      // Ella Boston Bag
            Map.entry("MWBGSEA03QA001", WearType.IN_HAND),      // Ella Boston Bag
            Map.entry("MWCGAAK01CO001", WearType.IN_HAND),      // Diamond Clutch
            Map.entry("MWRGATA05BK001", WearType.IN_HAND),      // Aren Vanity Case
            Map.entry("MWRGSTA01BK001", WearType.IN_HAND),      // Aren Vanity Case
            Map.entry("MWPAATN04BK001", WearType.IN_HAND),      // Toni Top-Zip Shopper — 작아서 손에 든다

            // 허리
            Map.entry("MMZGSFI01BK001", WearType.WAIST),        // Fursten Belt Bag
            Map.entry("MMZGSFI02BK001", WearType.WAIST),        // Fursten Belt Bag

            // 옆에 세움 — 캐리어
            Map.entry("MMVGSTT04BK001", WearType.BESIDE)        // Ottomar Cabin Trolley
    );

    /** 표에도 키워드에도 걸리지 않을 때 쓰는 기본값 */
    private static final WearType DEFAULT_WEAR_TYPE = WearType.ONE_SHOULDER;

    /**
     * SKU 매핑표를 먼저 보고, 없으면 제품명 키워드로 정한다.
     * 크롤링이 추가돼 표에 없는 SKU가 들어와도 적재가 멈추지 않도록 폴백을 둔다.
     */
    public WearType classify(String sku, String name) {
        WearType mapped = SKU_MAPPING.get(sku);
        if (mapped != null) {
            return mapped;
        }
        return classifyByName(name);
    }

    /**
     * 제품명 키워드 폴백. 표에 없는 새 SKU를 대략이라도 분류한다.
     * 정확도가 필요한 제품은 SKU_MAPPING에 직접 추가하는 것이 원칙이다.
     */
    private WearType classifyByName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_WEAR_TYPE;
        }

        String lowered = name.toLowerCase(Locale.ROOT);

        if (lowered.contains("belt bag") || lowered.contains("waist")) {
            return WearType.WAIST;
        }
        if (lowered.contains("trolley") || lowered.contains("luggage") || lowered.contains("suitcase")) {
            return WearType.BESIDE;
        }
        if (lowered.contains("sling") || lowered.contains("crossbody")) {
            return WearType.CROSSBODY;
        }
        if (lowered.contains("clutch") || lowered.contains("boston")
                || lowered.contains("weekender") || lowered.contains("vanity")
                || lowered.contains("garment")) {
            return WearType.IN_HAND;
        }
        // 백팩은 등 뒤가 안 보이므로 한쪽 어깨로 분류한다 (ADR 0001) — 기본값과 같다
        return DEFAULT_WEAR_TYPE;
    }
}
