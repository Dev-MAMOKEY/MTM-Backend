package com.likelion.mtm.infra.imagegen;

import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.openai.client.OpenAIClient;
import com.openai.models.images.Image;
import com.openai.models.images.ImageEditParams;
import com.openai.models.images.ImagesResponse;
import com.openai.services.blocking.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 네트워크 호출 없이 OpenAI 이미지 편집 요청 변환과 응답 파싱을 검증한다.
 */
class OpenAiImageGatewayTest {

    private OpenAIClient client;
    private ImageService imageService;
    private OpenAiImageGateway gateway;

    @BeforeEach
    void setUp() {
        client = mock(OpenAIClient.class);
        imageService = mock(ImageService.class);
        gateway = new OpenAiImageGateway(client, "gpt-image-2");
    }

    @Test
    @DisplayName("입력 이미지와 프롬프트 및 모델을 Image Edit API에 전달한다")
    void editSourceImage() throws IOException {
        byte[] generatedBytes = pngBytes("generated-image");
        AtomicReference<byte[]> uploadedBytes = new AtomicReference<>();
        AtomicReference<Path> temporaryImage = new AtomicReference<>();
        ImagesResponse response = responseWithBase64(
                Base64.getEncoder().encodeToString(generatedBytes)
        );
        when(client.images()).thenReturn(imageService);
        when(imageService.edit(org.mockito.ArgumentMatchers.any(ImageEditParams.class)))
                .thenAnswer(invocation -> {
                    ImageEditParams params = invocation.getArgument(0);
                    String filename = params._image().filename().orElseThrow();
                    temporaryImage.set(Path.of(System.getProperty("java.io.tmpdir"), filename));
                    assertThat(Files.exists(temporaryImage.get())).isTrue();
                    uploadedBytes.set(params.image().asInputStream().readAllBytes());
                    return response;
                });

        GeneratedImage result = gateway.generate(new ImageGenerationRequest(
                List.of(new ImageInput("source-image".getBytes(), "image/jpeg")),
                "make a base image"
        ));

        ArgumentCaptor<ImageEditParams> captor = ArgumentCaptor.forClass(ImageEditParams.class);
        verify(imageService).edit(captor.capture());
        ImageEditParams params = captor.getValue();

        assertThat(params.prompt()).isEqualTo("make a base image");
        assertThat(params.model().orElseThrow().asString()).isEqualTo("gpt-image-2");
        assertThat(params.inputFidelity()).isEmpty();
        assertThat(params.background()).contains(ImageEditParams.Background.OPAQUE);
        assertThat(params.outputFormat()).contains(ImageEditParams.OutputFormat.PNG);
        assertThat(params.image().isInputStream()).isTrue();
        assertThat(params._image().filename().orElseThrow()).endsWith(".jpg");
        assertThat(params._image().contentType()).isEqualTo("image/jpeg");
        assertThat(uploadedBytes.get()).isEqualTo("source-image".getBytes());
        assertThat(Files.notExists(temporaryImage.get())).isTrue();
        assertThat(result.data()).isEqualTo(generatedBytes);
        assertThat(result.mimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("여러 입력 이미지를 순서대로 OpenAI 편집 요청에 전달한다")
    void passMultipleImages() throws IOException {
        ImageGenerationRequest request = new ImageGenerationRequest(
                List.of(
                        new ImageInput("first".getBytes(), "image/png"),
                        new ImageInput("second".getBytes(), "image/png")
                ),
                "prompt"
        );
        List<Path> temporaryImages = gateway.createTemporaryImages(request.images());

        try {
            ImageEditParams params = gateway.toParams(request, temporaryImages);

            assertThat(params.image().isInputStreams()).isTrue();
            assertThat(params._image().filename()).isPresent();
            assertThat(params._image().contentType()).isEqualTo("image/png");
            assertThat(params.image().asInputStreams()).hasSize(2);
            assertThat(params.image().asInputStreams().get(0).readAllBytes())
                    .isEqualTo("first".getBytes());
            assertThat(params.image().asInputStreams().get(1).readAllBytes())
                    .isEqualTo("second".getBytes());
            for (var inputStream : params.image().asInputStreams()) {
                inputStream.close();
            }
        } finally {
            gateway.deleteTemporaryImages(temporaryImages);
        }
    }

    @Test
    @DisplayName("JPEG, PNG, WEBP 입력을 실제 이미지 MIME 타입의 file part로 구성한다")
    void useActualMultipartImageContentType() throws IOException {
        assertMultipartImage("jpeg-content".getBytes(), "image/jpeg", ".jpg");
        assertMultipartImage("png-content".getBytes(), "image/png", ".png");
        assertMultipartImage("webp-content".getBytes(), "image/webp", ".webp");
    }

    @Test
    @DisplayName("모델이 명시적으로 지원할 때만 high input fidelity를 요청한다")
    void useHighInputFidelityOnlyForSupportedModels() throws IOException {
        assertModelOptions("gpt-image-2", false);
        assertModelOptions("gpt-image-1-mini", false);
        assertModelOptions("gpt-image-1.5", true);
    }

    @Test
    @DisplayName("JPEG와 PNG 혼합 입력을 공통 PNG 형식으로 변환해 OpenAI에 전달한다")
    void normalizeMixedJpegAndPngInputs() throws IOException {
        byte[] generatedBytes = pngBytes("generated-image");
        ImagesResponse response = responseWithBase64(
                Base64.getEncoder().encodeToString(generatedBytes)
        );
        when(client.images()).thenReturn(imageService);
        when(imageService.edit(org.mockito.ArgumentMatchers.any(ImageEditParams.class)))
                .thenAnswer(invocation -> {
                    ImageEditParams params = invocation.getArgument(0);
                    assertThat(params._image().contentType()).isEqualTo("image/png");
                    assertThat(params._image().filename().orElseThrow()).endsWith(".png");
                    assertThat(params.image().isInputStreams()).isTrue();
                    assertThat(params.image().asInputStreams()).hasSize(2);
                    for (var inputStream : params.image().asInputStreams()) {
                        assertThat(inputStream.readNBytes(8)).isEqualTo(new byte[]{
                                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
                        });
                    }
                    return response;
                });

        GeneratedImage generatedImage = gateway.generate(new ImageGenerationRequest(
                List.of(
                        new ImageInput(imageBytes("jpeg"), "image/jpeg"),
                        new ImageInput(imageBytes("png"), "image/png")
                ),
                "prompt"
        ));

        assertThat(generatedImage.data()).isEqualTo(generatedBytes);
        verify(imageService).edit(org.mockito.ArgumentMatchers.any(ImageEditParams.class));
    }

    @Test
    @DisplayName("WEBP와 PNG 혼합 입력도 공통 PNG 형식으로 변환한다")
    void normalizeMixedWebpAndPngInputs() throws IOException {
        byte[] generatedBytes = pngBytes("generated-image");
        ImagesResponse response = responseWithBase64(
                Base64.getEncoder().encodeToString(generatedBytes)
        );
        when(client.images()).thenReturn(imageService);
        when(imageService.edit(org.mockito.ArgumentMatchers.any(ImageEditParams.class)))
                .thenAnswer(invocation -> {
                    ImageEditParams params = invocation.getArgument(0);
                    assertThat(params._image().contentType()).isEqualTo("image/png");
                    assertThat(params.image().asInputStreams()).hasSize(2);
                    return response;
                });

        GeneratedImage generatedImage = gateway.generate(new ImageGenerationRequest(
                List.of(
                        new ImageInput(webpBytes(), "image/webp"),
                        new ImageInput(imageBytes("png"), "image/png")
                ),
                "prompt"
        ));

        assertThat(generatedImage.data()).isEqualTo(generatedBytes);
        verify(imageService).edit(org.mockito.ArgumentMatchers.any(ImageEditParams.class));
    }

    @Test
    @DisplayName("입력 MIME 타입에 맞는 확장자와 원본 내용으로 임시 파일을 만든다")
    void createTemporaryImagesWithMimeExtension() throws IOException {
        List<ImageInput> images = List.of(
                new ImageInput("png".getBytes(), "image/png"),
                new ImageInput("jpeg".getBytes(), "image/jpeg"),
                new ImageInput("jpg".getBytes(), "image/jpg"),
                new ImageInput("webp".getBytes(), "image/webp")
        );
        List<Path> temporaryImages = gateway.createTemporaryImages(images);

        try {
            assertThat(temporaryImages)
                    .extracting(path -> path.getFileName().toString())
                    .satisfiesExactly(
                            filename -> assertThat(filename).endsWith(".png"),
                            filename -> assertThat(filename).endsWith(".jpg"),
                            filename -> assertThat(filename).endsWith(".jpg"),
                            filename -> assertThat(filename).endsWith(".webp")
                    );
            for (int index = 0; index < images.size(); index++) {
                assertThat(Files.readAllBytes(temporaryImages.get(index)))
                        .isEqualTo(images.get(index).data());
            }
        } finally {
            gateway.deleteTemporaryImages(temporaryImages);
        }
    }

    @Test
    @DisplayName("이미지가 없는 OpenAI 응답을 거부한다")
    void rejectEmptyResponse() {
        ImagesResponse response = ImagesResponse.builder()
                .created(1L)
                .data(List.of())
                .build();

        assertThatThrownBy(() -> gateway.parseGeneratedImage(response))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_GENERATION_ERROR);
    }

    @Test
    @DisplayName("잘못된 base64 이미지 응답을 거부한다")
    void rejectInvalidBase64() {
        ImagesResponse response = responseWithBase64("not-valid-base64!");

        assertThatThrownBy(() -> gateway.parseGeneratedImage(response))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_GENERATION_ERROR);
    }

    @Test
    @DisplayName("응답 형식 메타데이터가 없어도 실제 PNG 이미지이면 허용한다")
    void acceptPngWithoutOutputFormatMetadata() {
        byte[] png = pngBytes("without-format");
        ImagesResponse response = responseWithBase64WithoutOutputFormat(
                Base64.getEncoder().encodeToString(png)
        );

        GeneratedImage generatedImage = gateway.parseGeneratedImage(response);

        assertThat(generatedImage.data()).isEqualTo(png);
        assertThat(generatedImage.mimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("응답 형식 메타데이터가 blank여도 실제 PNG 이미지이면 허용한다")
    void acceptPngWithBlankOutputFormatMetadata() {
        byte[] png = pngBytes("blank-format");
        ImagesResponse response = ImagesResponse.builder()
                .created(1L)
                .data(List.of(Image.builder()
                        .b64Json(Base64.getEncoder().encodeToString(png))
                        .build()))
                .outputFormat(ImagesResponse.OutputFormat.of(""))
                .build();

        GeneratedImage generatedImage = gateway.parseGeneratedImage(response);

        assertThat(generatedImage.data()).isEqualTo(png);
        assertThat(generatedImage.mimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("요청한 PNG와 다른 응답 이미지 형식을 거부한다")
    void rejectUnexpectedOutputFormat() {
        ImagesResponse response = ImagesResponse.builder()
                .created(1L)
                .data(List.of(Image.builder()
                        .b64Json(Base64.getEncoder().encodeToString(pngBytes("image")))
                        .build()))
                .outputFormat(ImagesResponse.OutputFormat.JPEG)
                .build();

        assertThatThrownBy(() -> gateway.parseGeneratedImage(response))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_GENERATION_ERROR);
    }

    @Test
    @DisplayName("Base64 디코딩 결과가 PNG signature와 다르면 거부한다")
    void rejectImageWithoutPngSignature() {
        ImagesResponse response = responseWithBase64(
                Base64.getEncoder().encodeToString("not-png".getBytes())
        );

        assertThatThrownBy(() -> gateway.parseGeneratedImage(response))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_GENERATION_ERROR);
    }

    @Test
    @DisplayName("OpenAI가 지원하지 않는 입력 이미지 MIME 타입을 호출 전에 거부한다")
    void rejectUnsupportedInputMimeType() {
        ImageGenerationRequest request = new ImageGenerationRequest(
                List.of(new ImageInput("gif-image".getBytes(), "image/gif")),
                "prompt"
        );

        assertThatThrownBy(() -> gateway.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_GENERATION_ERROR);
        verifyNoInteractions(imageService);
    }

    @Test
    @DisplayName("OpenAI SDK 예외를 외부에 노출하지 않고 생성 오류로 변환한다")
    void convertSdkFailure() {
        AtomicReference<Path> temporaryImage = new AtomicReference<>();
        when(client.images()).thenReturn(imageService);
        when(imageService.edit(org.mockito.ArgumentMatchers.any(ImageEditParams.class)))
                .thenAnswer(invocation -> {
                    ImageEditParams params = invocation.getArgument(0);
                    String filename = params._image().filename().orElseThrow();
                    temporaryImage.set(Path.of(System.getProperty("java.io.tmpdir"), filename));
                    assertThat(Files.exists(temporaryImage.get())).isTrue();
                    throw new RuntimeException("provider failure");
                });

        assertThatThrownBy(() -> gateway.generate(new ImageGenerationRequest(
                List.of(new ImageInput("source".getBytes(), "image/png")),
                "prompt"
        )))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_GENERATION_ERROR);
        assertThat(Files.notExists(temporaryImage.get())).isTrue();
    }

    private ImagesResponse responseWithBase64(String value) {
        return ImagesResponse.builder()
                .created(1L)
                .data(List.of(Image.builder().b64Json(value).build()))
                .outputFormat(ImagesResponse.OutputFormat.PNG)
                .build();
    }

    private ImagesResponse responseWithBase64WithoutOutputFormat(String value) {
        return ImagesResponse.builder()
                .created(1L)
                .data(List.of(Image.builder().b64Json(value).build()))
                .build();
    }

    private byte[] pngBytes(String payload) {
        byte[] signature = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        byte[] payloadBytes = payload.getBytes();
        byte[] png = new byte[signature.length + payloadBytes.length];
        System.arraycopy(signature, 0, png, 0, signature.length);
        System.arraycopy(payloadBytes, 0, png, signature.length, payloadBytes.length);
        return png;
    }

    private byte[] imageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        image.setRGB(1, 0, Color.GREEN.getRGB());
        image.setRGB(0, 1, Color.BLUE.getRGB());
        image.setRGB(1, 1, Color.WHITE.getRGB());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, outputStream)).isTrue();
        return outputStream.toByteArray();
    }

    private byte[] webpBytes() {
        return Base64.getDecoder().decode(
                "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA"
        );
    }

    private void assertMultipartImage(byte[] data, String mimeType, String suffix)
            throws IOException {
        ImageGenerationRequest request = new ImageGenerationRequest(
                List.of(new ImageInput(data, mimeType)),
                "prompt"
        );
        List<Path> temporaryImages = gateway.createTemporaryImages(request.images());

        try {
            ImageEditParams params = gateway.toParams(request, temporaryImages);

            assertThat(params.image().isInputStream()).isTrue();
            assertThat(params._image().filename().orElseThrow()).endsWith(suffix);
            assertThat(params._image().contentType()).isEqualTo(mimeType);
            assertThat(params.image().asInputStream().readAllBytes()).isEqualTo(data);
            params.image().asInputStream().close();
        } finally {
            gateway.deleteTemporaryImages(temporaryImages);
        }
    }

    private void assertModelOptions(String model, boolean expectsHighInputFidelity)
            throws IOException {
        OpenAiImageGateway modelGateway = new OpenAiImageGateway(client, model);
        ImageGenerationRequest request = new ImageGenerationRequest(
                List.of(new ImageInput("image".getBytes(), "image/png")),
                "prompt"
        );
        List<Path> temporaryImages = modelGateway.createTemporaryImages(request.images());

        try {
            ImageEditParams params = modelGateway.toParams(request, temporaryImages);

            if (expectsHighInputFidelity) {
                assertThat(params.inputFidelity()).contains(ImageEditParams.InputFidelity.HIGH);
            } else {
                assertThat(params.inputFidelity()).isEmpty();
            }
            assertThat(params.background()).contains(ImageEditParams.Background.OPAQUE);
            assertThat(params.outputFormat()).contains(ImageEditParams.OutputFormat.PNG);
            params.image().asInputStream().close();
        } finally {
            modelGateway.deleteTemporaryImages(temporaryImages);
        }
    }
}
