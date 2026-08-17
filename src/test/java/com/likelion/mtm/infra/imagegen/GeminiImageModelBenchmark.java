package com.likelion.mtm.infra.imagegen;

import com.google.genai.Client;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 실제 Gemini API를 호출해 후보 이미지 생성 모델들의 지연시간·성공 여부·결과 이미지를 비교하는
 * 수동 벤치마크. 과금이 발생하므로 일반 테스트 실행(CI 포함)에서는 제외되며,
 * 필요할 때 IDE에서 benchmarkCandidateModels()만 직접 실행한다.
 *
 * 실행 전 준비:
 *  - GEMINI_API_KEY를 환경변수로 설정하거나 프로젝트 루트 .env에 그대로 둔다 (자동으로 읽는다).
 *  - 모델당 반복 횟수를 늘리려면 VM 옵션에 -Dbenchmark.repeats=3 을 추가한다 (기본 1회).
 *
 * 실행 후: build/gemini-benchmark/ 아래에 모델별 결과 이미지가 저장되고,
 * 콘솔에 모델 x 시도별 지연시간·성공 여부 요약 표가 출력된다.
 */
class GeminiImageModelBenchmark {

    private static final List<String> CANDIDATE_MODELS = List.of(
            "gemini-3.1-flash-lite-image",
            "gemini-3.1-flash-image",
            "gemini-3-pro-image"
    );

    private static final Path SAMPLE_IMAGE = Path.of(
            "crawling-data", "MMKEAVE12BK001", "images", "MMKEAVE12BK001_01.webp"
    );

    private static final String PROMPT = """
            Generate a photorealistic image of a person naturally wearing or holding this product.
            Preserve the product's design, shape, color, material appearance, and distinctive details as faithfully as possible.
            Do not replace, redesign, or transform the product into a different product or an invented design.
            Produce one final image with no added text or watermark.
            """;

    private static final Path OUTPUT_DIR = Path.of("build", "gemini-benchmark");

    @Test
    @Disabled("수동 벤치마크: 실제 API 호출로 비용이 발생하므로 필요할 때 직접 실행한다.")
    void benchmarkCandidateModels() throws IOException {
        String apiKey = resolveApiKey();
        int repeats = Integer.getInteger("benchmark.repeats", 1);

        Files.createDirectories(OUTPUT_DIR);
        Client client = Client.builder().apiKey(apiKey).build();
        ImageGenerationRequest request = new ImageGenerationRequest(
                List.of(new ImageInput(Files.readAllBytes(SAMPLE_IMAGE), "image/webp")),
                PROMPT
        );

        List<Result> results = new ArrayList<>();
        for (String model : CANDIDATE_MODELS) {
            GeminiImageGateway gateway = new GeminiImageGateway(client, model);
            for (int attempt = 1; attempt <= repeats; attempt++) {
                results.add(runOnce(gateway, model, attempt, request));
            }
        }

        printSummary(results);
    }

    /**
     * 모델 한 번 호출을 지연시간과 함께 실행하고, 실패해도 나머지 모델 비교를 이어갈 수 있도록
     * 예외를 결과 값으로 흡수한다.
     */
    private Result runOnce(GeminiImageGateway gateway, String model, int attempt, ImageGenerationRequest request) {
        Instant start = Instant.now();
        try {
            GeneratedImage image = gateway.generate(request);
            Duration elapsed = Duration.between(start, Instant.now());
            Path savedPath = save(model, attempt, image);
            return Result.success(model, attempt, elapsed, image.data().length, image.mimeType(), savedPath);
        } catch (Exception e) {
            Duration elapsed = Duration.between(start, Instant.now());
            return Result.failure(model, attempt, elapsed, e);
        }
    }

    private Path save(String model, int attempt, GeneratedImage image) {
        try {
            String extension = "image/png".equals(image.mimeType()) ? "png" : "bin";
            Path path = OUTPUT_DIR.resolve("%s-%d.%s".formatted(model, attempt, extension));
            Files.write(path, image.data());
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printSummary(List<Result> results) {
        System.out.println();
        System.out.printf("%-30s %-8s %-10s %-4s %-10s %-10s %s%n",
                "MODEL", "ATTEMPT", "LATENCY", "OK", "BYTES", "MIME", "SAVED PATH / ERROR");
        for (Result r : results) {
            System.out.printf("%-30s %-8d %-10s %-4s %-10s %-10s %s%n",
                    r.model(), r.attempt(), r.latency().toMillis() + "ms", r.success() ? "O" : "X",
                    r.success() ? String.valueOf(r.bytes()) : "-",
                    r.success() ? r.mimeType() : "-",
                    r.success() ? r.savedPath() : r.error());
        }
    }

    /**
     * GEMINI_API_KEY를 환경변수에서 먼저 찾고, 없으면 프로젝트 루트 .env를 직접 읽는다.
     * (이 테스트는 Spring 컨텍스트 없이 실행되므로 application.yml의 자동 .env 로딩을 타지 않는다.)
     */
    private String resolveApiKey() throws IOException {
        String fromEnv = System.getenv("GEMINI_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        Path envFile = Path.of(".env");
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                if (line.startsWith("GEMINI_API_KEY=")) {
                    return line.substring("GEMINI_API_KEY=".length()).trim();
                }
            }
        }

        throw new IllegalStateException("GEMINI_API_KEY를 환경변수 또는 .env에서 찾을 수 없습니다.");
    }

    private record Result(
            String model, int attempt, Duration latency, boolean success,
            long bytes, String mimeType, String savedPath, String error
    ) {
        static Result success(String model, int attempt, Duration latency, long bytes, String mimeType, Path path) {
            return new Result(model, attempt, latency, true, bytes, mimeType, path.toString(), null);
        }

        static Result failure(String model, int attempt, Duration latency, Exception e) {
            return new Result(model, attempt, latency, false, 0, null, null, e.getMessage());
        }
    }
}
