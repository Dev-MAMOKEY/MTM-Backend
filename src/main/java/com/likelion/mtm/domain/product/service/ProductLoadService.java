package com.likelion.mtm.domain.product.service;

import com.likelion.mtm.domain.product.entity.Currency;
import com.likelion.mtm.domain.product.entity.Dimensions;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.ProductCut;
import com.likelion.mtm.domain.product.entity.WearType;
import com.likelion.mtm.domain.product.repository.ProductCutRepository;
import com.likelion.mtm.domain.product.repository.ProductRepository;
import com.likelion.mtm.infra.storage.ImageData;
import com.likelion.mtm.infra.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 크롤링 SKU 한 건을 적재하는 서비스.
 *
 * 제품과 제품 컷을 한 트랜잭션으로 묶는다. 이미지만 저장소에 올라가고 DB가 비면
 * 다음 실행에서 SKU 중복으로 건너뛰어 깨진 채로 남기 때문이다.
 * (적재기 ApplicationRunner와 분리한 이유 — 같은 클래스 안에서 부르면 프록시를 타지 않아 @Transactional이 걸리지 않는다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLoadService {

    /** SKU 앞 11자 = base_sku. 사이즈 변형끼리 제품 컷이 같아 이미지 매칭에 쓴다 */
    private static final int BASE_SKU_LENGTH = 11;

    /** 크롤링 이미지가 전부 webp다 */
    private static final String WEBP_MIME = "image/webp";

    private static final String CUT_DIRECTORY_PREFIX = "product-cuts/";

    /** 슬롯 1번을 목록 대표 이미지(정면 컷)로 쓴다 */
    private static final int FRONT_SLOT_NO = 1;

    private final ProductRepository productRepository;
    private final ProductCutRepository productCutRepository;
    private final DimensionParser dimensionParser;
    private final WearTypeClassifier wearTypeClassifier;
    private final ImageStorage imageStorage;
    private final ObjectMapper objectMapper;

    /** 적재기가 실행 요약을 찍을 수 있도록 처리 결과를 알려준다 */
    public enum LoadResult {
        LOADED,
        SKIPPED_ALREADY_EXISTS,
        SKIPPED_NO_META,
        SKIPPED_NO_DIMENSIONS
    }

    /**
     * crawling-data/{SKU} 디렉터리 하나를 읽어 제품과 제품 컷을 저장한다.
     * 이미 있는 SKU는 건너뛰므로 여러 번 실행해도 중복이 생기지 않는다.
     */
    @Transactional
    public LoadResult load(Path skuDir) throws IOException {

        Path metaPath = skuDir.resolve("meta.json");
        if (!Files.exists(metaPath)) {
            log.warn("meta.json이 없어 건너뜁니다: {}", skuDir);
            return LoadResult.SKIPPED_NO_META;
        }

        JsonNode meta = objectMapper.readTree(Files.readString(metaPath, StandardCharsets.UTF_8));

        String sku = textOrNull(meta, "sku");
        if (sku == null || sku.isBlank()) {
            log.warn("sku가 비어 있어 건너뜁니다: {}", skuDir);
            return LoadResult.SKIPPED_NO_META;
        }

        if (productRepository.existsBySku(sku)) {
            return LoadResult.SKIPPED_ALREADY_EXISTS;
        }

        // 실측 치수가 없으면 착용 이미지 프롬프트를 만들 수 없으므로 적재하지 않는다
        Optional<Dimensions> dimensions = dimensionParser.parse(readDetails(meta));
        if (dimensions.isEmpty()) {
            log.warn("실측 치수를 찾지 못해 건너뜁니다: {}", sku);
            return LoadResult.SKIPPED_NO_DIMENSIONS;
        }

        Product product = productRepository.save(toProduct(meta, sku, dimensions.get()));
        int cutCount = storeProductCuts(product, skuDir, sku);

        log.info("적재 완료: {} ({}) — 제품 컷 {}장", sku, product.getWearType(), cutCount);
        return LoadResult.LOADED;
    }

    /** meta.json 한 건을 Product로 옮긴다 */
    private Product toProduct(JsonNode meta, String sku, Dimensions dimensions) {
        String name = textOrNull(meta, "name");
        WearType wearType = wearTypeClassifier.classify(sku, name);

        return Product.of(
                sku,
                sku.length() >= BASE_SKU_LENGTH ? sku.substring(0, BASE_SKU_LENGTH) : sku,
                name,
                textOrNull(meta, "color"),
                new BigDecimal(textOrNull(meta, "price")),
                // 크롤링 원본이 미국 사이트라 USD가 들어온다. 환산하지 않고 그대로 저장한다
                Currency.valueOf(textOrNull(meta, "currency")),
                textOrNull(meta, "description"),
                dimensions,
                wearType,
                textOrNull(meta, "url")
        );
    }

    /**
     * images/ 안의 파일을 저장소에 올리고 제품 컷으로 기록한다.
     * is_worn_slot(모델 착용 컷)은 크롤링 데이터에 판정 근거가 없어 전부 false로 둔다 — 별도 이슈에서 지정한다.
     */
    private int storeProductCuts(Product product, Path skuDir, String sku) throws IOException {
        Path imageDir = skuDir.resolve("images");
        if (!Files.isDirectory(imageDir)) {
            log.warn("images 디렉터리가 없습니다: {}", sku);
            return 0;
        }

        List<Path> imageFiles;
        try (Stream<Path> files = Files.list(imageDir)) {
            imageFiles = files.filter(Files::isRegularFile).sorted().toList();
        }

        for (Path imageFile : imageFiles) {
            byte[] bytes = Files.readAllBytes(imageFile);
            int slotNo = parseSlotNo(imageFile.getFileName().toString());

            String storageKey = imageStorage.store(
                    new ImageData(bytes, WEBP_MIME),
                    CUT_DIRECTORY_PREFIX + sku
            );

            productCutRepository.save(
                    ProductCut.of(product, storageKey, slotNo, slotNo == FRONT_SLOT_NO, false)
            );
        }

        return imageFiles.size();
    }

    /**
     * 파일명 끝의 슬롯 번호를 뽑는다. 예) MMLGATA02BK001_01.webp -> 1
     * 형식이 다르면 정면 컷으로 오인하지 않도록 0을 준다.
     */
    private int parseSlotNo(String fileName) {
        int underscore = fileName.lastIndexOf('_');
        int dot = fileName.lastIndexOf('.');

        if (underscore < 0 || dot < 0 || underscore >= dot) {
            return 0;
        }

        try {
            return Integer.parseInt(fileName.substring(underscore + 1, dot));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 실측 치수 파서의 입력 — 치수 문장은 description이 아니라 details 배열 안에 있다 */
    private List<String> readDetails(JsonNode meta) {
        List<String> details = new ArrayList<>();
        for (JsonNode node : meta.path("details")) {
            if (!node.isNull()) {
                details.add(node.asString());
            }
        }
        return details;
    }

    /**
     * 값이 없거나 JSON null이면 null을 준다.
     * asString()을 그대로 쓰면 null 노드가 "null" 문자열이 되어 DB에 들어간다.
     */
    private String textOrNull(JsonNode parent, String fieldName) {
        JsonNode node = parent.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asString();
    }
}
