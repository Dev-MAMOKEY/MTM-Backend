package com.likelion.mtm.infra.imagegen;

import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.openai.client.OpenAIClient;
import com.openai.core.MultipartField;
import com.openai.models.images.ImageEditParams;
import com.openai.models.images.ImageModel;
import com.openai.models.images.ImagesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * OpenAI Image Edit API를 사용하는 이미지 생성 게이트웨이 구현체.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "image-generation", name = "provider", havingValue = "OPENAI")
public class OpenAiImageGateway implements ImageGenerationGateway {

    private static final String OUTPUT_MIME_TYPE = "image/png";
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final Set<String> SUPPORTED_INPUT_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    private final OpenAIClient client;
    private final String model;

    /**
     * OpenAI 클라이언트와 호출할 이미지 모델을 주입받는다.
     */
    public OpenAiImageGateway(
            OpenAIClient client,
            @Value("${openai.model}") String model
    ) {
        this.client = client;
        this.model = model;
    }

    /**
     * 입력 이미지를 OpenAI Image Edit API로 변환하고 생성된 PNG 이미지를 반환한다.
     */
    @Override
    public GeneratedImage generate(ImageGenerationRequest request) {
        List<Path> temporaryImages = List.of();
        ImageEditParams params = null;

        try {
            validateInputMimeTypes(request.images());
            List<ImageInput> normalizedImages = normalizeMixedInputFormats(request.images());
            ImageGenerationRequest normalizedRequest = new ImageGenerationRequest(
                    normalizedImages,
                    request.prompt()
            );
            temporaryImages = createTemporaryImages(normalizedImages);
            params = toParams(normalizedRequest, temporaryImages);
            ImagesResponse response = callOpenAi(params);
            GeneratedImage generatedImage = parseGeneratedImage(response);
            log.info("OpenAI 이미지 생성에 성공했습니다. model={}, mimeType={}",
                    model, generatedImage.mimeType());
            return generatedImage;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI 이미지 생성에 실패했습니다.", e);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        } finally {
            closeImageStreams(params);
            deleteTemporaryImages(temporaryImages);
        }
    }

    /**
     * OpenAI SDK 호출 실패를 다른 생성 단계와 구분해 기록한다.
     */
    private ImagesResponse callOpenAi(ImageEditParams params) {
        try {
            return client.images().edit(params);
        } catch (Exception e) {
            log.error("OpenAI SDK 이미지 편집 호출에 실패했습니다. model={}", model, e);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }
    }

    /**
     * 공급자 중립 요청과 임시 이미지 파일을 OpenAI 이미지 편집 요청으로 변환한다.
     */
    ImageEditParams toParams(ImageGenerationRequest request, List<Path> temporaryImages)
            throws IOException {
        if (request.images().size() != temporaryImages.size() || temporaryImages.isEmpty()) {
            log.warn("OpenAI 이미지 편집 요청 구성에 실패했습니다. imageCount={}, temporaryFileCount={}",
                    request.images().size(), temporaryImages.size());
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }

        ImageEditParams.Builder builder = ImageEditParams.builder()
                .prompt(request.prompt())
                .model(ImageModel.of(model))
                .background(ImageEditParams.Background.OPAQUE)
                .outputFormat(ImageEditParams.OutputFormat.PNG)
                .n(1);

        builder.image(createImageField(request.images(), temporaryImages));

        if (supportsExplicitHighInputFidelity(model)) {
            builder.inputFidelity(ImageEditParams.InputFidelity.HIGH);
        }

        return builder.build();
    }

    /**
     * 명시적인 high input fidelity 설정을 지원하는 OpenAI 이미지 모델인지 확인한다.
     */
    private boolean supportsExplicitHighInputFidelity(String model) {
        return "gpt-image-1".equals(model) || model.startsWith("gpt-image-1.5");
    }

    /**
     * filename과 실제 이미지 MIME 타입을 포함하는 SDK multipart file 필드를 만든다.
     */
    private MultipartField<ImageEditParams.Image> createImageField(
            List<ImageInput> images,
            List<Path> temporaryImages
    ) throws IOException {
        String contentType = commonMultipartContentType(images);
        List<InputStream> inputStreams = openInputStreams(temporaryImages);
        ImageEditParams.Image image = inputStreams.size() == 1
                ? ImageEditParams.Image.ofInputStream(inputStreams.get(0))
                : ImageEditParams.Image.ofInputStreams(inputStreams);

        return MultipartField.<ImageEditParams.Image>builder()
                .value(image)
                .contentType(contentType)
                .filename(temporaryImages.get(0).getFileName().toString())
                .build();
    }

    /**
     * 서로 다른 이미지 형식을 SDK 배열 필드가 공유할 수 있도록 PNG 형식으로 통일한다.
     */
    private List<ImageInput> normalizeMixedInputFormats(List<ImageInput> images) {
        String firstMimeType = multipartContentType(images.get(0).mimeType());
        boolean hasDifferentMimeType = images.stream()
                .map(ImageInput::mimeType)
                .map(this::multipartContentType)
                .anyMatch(mimeType -> !mimeType.equals(firstMimeType));

        if (!hasDifferentMimeType) {
            return images;
        }

        return images.stream()
                .map(this::convertToPng)
                .toList();
    }

    /**
     * OpenAI 다중 이미지 요청에서 공통 MIME 타입을 사용할 수 있도록 입력 이미지를 PNG로 변환한다.
     */
    private ImageInput convertToPng(ImageInput image) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image.data()));
            if (bufferedImage == null) {
                log.warn("OpenAI 입력 이미지를 PNG로 변환할 수 없습니다. mimeType={}",
                        image.mimeType());
                throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!ImageIO.write(bufferedImage, "png", outputStream)) {
                log.warn("OpenAI 입력 이미지의 PNG 인코더를 찾을 수 없습니다.");
                throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
            }
            return new ImageInput(outputStream.toByteArray(), "image/png");
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            log.warn("OpenAI 입력 이미지의 PNG 변환에 실패했습니다. mimeType={}",
                    image.mimeType(), e);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }
    }

    /**
     * SDK의 배열 이미지 필드가 공유할 실제 MIME 타입을 확인한다.
     * mixed 입력은 임시 파일 생성 전에 공통 PNG 형식으로 정규화되어야 한다.
     */
    private String commonMultipartContentType(List<ImageInput> images) {
        String contentType = multipartContentType(images.get(0).mimeType());
        boolean hasDifferentMimeType = images.stream()
                .map(ImageInput::mimeType)
                .map(this::multipartContentType)
                .anyMatch(mimeType -> !mimeType.equals(contentType));

        if (hasDifferentMimeType) {
            log.warn("OpenAI 다중 이미지 입력의 MIME 타입이 서로 다릅니다. imageCount={}",
                    images.size());
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }
        return contentType;
    }

    /**
     * OpenAI가 허용하는 표준 multipart 이미지 MIME 타입으로 정규화한다.
     */
    private String multipartContentType(String mimeType) {
        return "image/jpg".equals(mimeType) ? "image/jpeg" : mimeType;
    }

    /**
     * 입력 MIME 타입을 OpenAI가 지원하는 이미지 형식으로 제한한다.
     */
    private void validateInputMimeTypes(List<ImageInput> images) {
        List<String> unsupportedMimeTypes = images.stream()
                .map(ImageInput::mimeType)
                .filter(mimeType -> !SUPPORTED_INPUT_MIME_TYPES.contains(mimeType))
                .distinct()
                .toList();

        if (!unsupportedMimeTypes.isEmpty()) {
            log.warn("OpenAI가 지원하지 않는 이미지 MIME 타입입니다. mimeTypes={}",
                    unsupportedMimeTypes);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }
    }

    /**
     * SDK가 filename을 포함한 file part를 만들 수 있도록 입력 이미지를 임시 파일로 저장한다.
     */
    List<Path> createTemporaryImages(List<ImageInput> images) throws IOException {
        List<Path> temporaryImages = new ArrayList<>();

        try {
            for (ImageInput image : images) {
                Path temporaryImage = Files.createTempFile("mtm-openai-image-", extension(image.mimeType()));
                Files.write(temporaryImage, image.data());
                temporaryImages.add(temporaryImage);
            }
            return List.copyOf(temporaryImages);
        } catch (IOException | RuntimeException e) {
            deleteTemporaryImages(temporaryImages);
            log.error("OpenAI 요청용 임시 이미지 파일 생성에 실패했습니다. imageCount={}",
                    images.size(), e);
            throw e;
        }
    }

    /**
     * 이미지 MIME 타입에 대응하는 안전한 임시 파일 확장자를 반환한다.
     */
    private String extension(String mimeType) {
        return switch (mimeType) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        };
    }

    /**
     * 다중 이미지 multipart 요청에 사용할 파일 스트림을 연다.
     */
    private List<InputStream> openInputStreams(List<Path> temporaryImages) throws IOException {
        List<InputStream> inputStreams = new ArrayList<>();

        try {
            for (Path temporaryImage : temporaryImages) {
                inputStreams.add(Files.newInputStream(temporaryImage));
            }
            return List.copyOf(inputStreams);
        } catch (IOException | RuntimeException e) {
            closeInputStreams(inputStreams);
            throw e;
        }
    }

    /**
     * 요청 객체가 보유한 이미지 파일 스트림을 닫는다.
     */
    private void closeImageStreams(ImageEditParams params) {
        if (params == null) {
            return;
        }

        ImageEditParams.Image image = params.image();
        if (image.isInputStream()) {
            closeInputStreams(List.of(image.asInputStream()));
        } else if (image.isInputStreams()) {
            closeInputStreams(image.asInputStreams());
        }
    }

    /**
     * 이미지 파일 스트림을 닫되 정리 실패로 원래 호출 결과를 변경하지 않는다.
     */
    private void closeInputStreams(List<InputStream> inputStreams) {
        for (InputStream inputStream : inputStreams) {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("OpenAI 임시 이미지 스트림 정리에 실패했습니다.", e);
            }
        }
    }

    /**
     * OpenAI 요청에 사용한 임시 이미지 파일을 모두 삭제한다.
     */
    void deleteTemporaryImages(List<Path> temporaryImages) {
        for (Path temporaryImage : temporaryImages) {
            try {
                Files.deleteIfExists(temporaryImage);
            } catch (IOException e) {
                log.warn("OpenAI 임시 이미지 파일 정리에 실패했습니다.", e);
            }
        }
    }

    /**
     * OpenAI 응답의 첫 번째 base64 이미지를 디코딩한다.
     */
    GeneratedImage parseGeneratedImage(ImagesResponse response) {
        String responseFormat = response.outputFormat()
                .map(ImagesResponse.OutputFormat::asString)
                .orElse("");
        if (!responseFormat.isBlank() && !"png".equals(responseFormat)) {
            log.warn("OpenAI 이미지 응답 형식이 예상과 다릅니다. expected={}, actual={}",
                    ImagesResponse.OutputFormat.PNG, responseFormat);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }

        List<com.openai.models.images.Image> images = response.data().orElse(List.of());
        if (images.isEmpty()) {
            log.warn("OpenAI 이미지 응답에 data가 없습니다. model={}", model);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }

        String encodedImage = images.stream()
                .flatMap(image -> image.b64Json().stream())
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
        if (encodedImage == null) {
            log.warn("OpenAI 이미지 응답에 유효한 b64_json이 없습니다. model={}", model);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }

        try {
            byte[] imageData = Base64.getDecoder().decode(encodedImage);
            if (imageData.length == 0) {
                log.warn("OpenAI 이미지 응답의 디코딩 결과가 비어 있습니다. model={}", model);
                throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
            }
            if (!hasPngSignature(imageData)) {
                log.warn("OpenAI 이미지 응답이 PNG signature와 일치하지 않습니다. model={}", model);
                throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
            }
            return new GeneratedImage(imageData, OUTPUT_MIME_TYPE);
        } catch (IllegalArgumentException e) {
            log.warn("OpenAI 이미지 응답의 Base64 디코딩에 실패했습니다. model={}", model, e);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }
    }

    /**
     * 디코딩된 이미지가 PNG 파일 signature로 시작하는지 확인한다.
     */
    private boolean hasPngSignature(byte[] imageData) {
        if (imageData.length < PNG_SIGNATURE.length) {
            return false;
        }

        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (imageData[index] != PNG_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }
}
