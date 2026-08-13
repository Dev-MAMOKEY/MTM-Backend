package com.likelion.mtm.domain.product.service;

import com.likelion.mtm.domain.product.entity.Dimensions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 실측 치수 파서 — 제품 상세의 "Approximately D x W x H inches"에서 치수를 뽑는다.
 *
 * 크롤링 데이터에서 이 문장은 description이 아니라 details 배열 안에 들어 있다.
 * ("Made in Italy", "Interior pocket" 같은 다른 항목들과 섞여 있어 한 줄씩 훑어야 한다)
 */
@Component
public class DimensionParser {

    /** 깊이 x 너비 x 높이 순서. 소수점을 포함한다 */
    private static final Pattern PATTERN =
            Pattern.compile("Approximately\\s+([\\d.]+)\\s*x\\s*([\\d.]+)\\s*x\\s*([\\d.]+)\\s*inches");

    /**
     * details 목록에서 치수 문장을 찾아 파싱한다.
     * 못 찾으면 비어 있는 Optional을 준다 — 적재기가 해당 SKU를 건너뛰고 경고 로그를 남긴다.
     * (치수가 없으면 착용 이미지 프롬프트를 만들 수 없다)
     */
    public Optional<Dimensions> parse(List<String> details) {
        if (details == null || details.isEmpty()) {
            return Optional.empty();
        }

        for (String detail : details) {
            if (detail == null) {
                continue;
            }

            Matcher matcher = PATTERN.matcher(detail);
            if (matcher.find()) {
                return Optional.of(Dimensions.of(
                        new BigDecimal(matcher.group(1)),
                        new BigDecimal(matcher.group(2)),
                        new BigDecimal(matcher.group(3))
                ));
            }
        }

        return Optional.empty();
    }
}
