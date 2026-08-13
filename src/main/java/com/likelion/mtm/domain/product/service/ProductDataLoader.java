package com.likelion.mtm.domain.product.service;

import com.likelion.mtm.domain.product.service.ProductLoadService.LoadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 크롤링 제품 적재기 — crawling-data/{SKU}/ 를 순회하며 제품을 DB와 이미지 저장소에 넣는다.
 *
 * 매 기동마다 돌면 안 되므로 custom.crawling.enabled=true 일 때만 빈으로 등록된다.
 * 적재는 SKU 단위로 이미 있는 것을 건너뛰므로 여러 번 실행해도 안전하다
 * (로컬과 RDS에 각각 한 번씩 실행한다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "custom.crawling.enabled", havingValue = "true")
public class ProductDataLoader implements ApplicationRunner {

    private final ProductLoadService productLoadService;

    @Value("${custom.crawling.path}")
    private String crawlingPath;

    @Override
    public void run(ApplicationArguments args) {
        Path root = Path.of(crawlingPath);

        if (!Files.isDirectory(root)) {
            log.error("크롤링 데이터 디렉터리를 찾을 수 없습니다: {}", root.toAbsolutePath());
            return;
        }

        List<Path> skuDirs;
        try (Stream<Path> paths = Files.list(root)) {
            skuDirs = paths.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            log.error("크롤링 데이터 디렉터리를 읽지 못했습니다: {}", root.toAbsolutePath(), e);
            return;
        }

        log.info("크롤링 제품 적재를 시작합니다 — 대상 {}건", skuDirs.size());

        Map<LoadResult, Integer> counts = new EnumMap<>(LoadResult.class);
        int failed = 0;

        for (Path skuDir : skuDirs) {
            try {
                LoadResult result = productLoadService.load(skuDir);
                counts.merge(result, 1, Integer::sum);
            } catch (Exception e) {
                // SKU 하나가 실패해도 나머지는 계속 적재한다. 실패분은 로그를 보고 다시 실행하면 된다
                failed++;
                log.error("적재에 실패했습니다: {}", skuDir.getFileName(), e);
            }
        }

        log.info("크롤링 제품 적재 완료 — 신규 {}건, 이미 있음 {}건, 치수 없음 {}건, 메타 없음 {}건, 실패 {}건",
                counts.getOrDefault(LoadResult.LOADED, 0),
                counts.getOrDefault(LoadResult.SKIPPED_ALREADY_EXISTS, 0),
                counts.getOrDefault(LoadResult.SKIPPED_NO_DIMENSIONS, 0),
                counts.getOrDefault(LoadResult.SKIPPED_NO_META, 0),
                failed);
    }
}
