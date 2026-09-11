package com.zipdamember.domain.agent.ocr;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.AsyncAnnotateFileRequest;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.GcsDestination;
import com.google.cloud.vision.v1.GcsSource;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageContext;
import com.google.cloud.vision.v1.InputConfig;
import com.google.cloud.vision.v1.OutputConfig;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import com.zipdamember.global.config.ocr.OcrConfig;
import com.zipdamember.global.error.custom.business.OcrProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GoogleCloudVisionTextExtractor implements DocumentTextExtractor {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final OcrConfig ocrConfig;

    @Override
    public String extract(byte[] content, String contentType) {
        validateConfiguration(content);
        if (PDF_CONTENT_TYPE.equals(contentType)) {
            return extractPdf(content);
        }
        return extractImage(content);
    }

    private String extractImage(byte[] content) {
        Feature feature = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();
        ImageContext imageContext = ImageContext.newBuilder()
                .addLanguageHints("ko")
                .addLanguageHints("en")
                .build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .setImage(Image.newBuilder().setContent(ByteString.copyFrom(content)).build())
                .addFeatures(feature)
                .setImageContext(imageContext)
                .build();

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            AnnotateImageResponse response = client.batchAnnotateImages(List.of(request))
                    .getResponses(0);
            validateVisionResponse(response);
            return requireText(response.getFullTextAnnotation().getText());
        } catch (OcrProcessingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OcrProcessingException("이미지 OCR 처리에 실패했습니다.", exception);
        }
    }

    private String extractPdf(byte[] content) {
        validatePdfBuckets();
        String jobId = UUID.randomUUID().toString();
        String inputObject = "zipda-ocr/input/" + jobId + ".pdf";
        String outputPrefix = "zipda-ocr/output/" + jobId + "/";
        Storage storage = createStorage();
        List<BlobId> temporaryObjects = new ArrayList<>();

        try {
            BlobId inputBlobId = BlobId.of(ocrConfig.inputBucket(), inputObject);
            storage.create(
                    BlobInfo.newBuilder(inputBlobId).setContentType(PDF_CONTENT_TYPE).build(),
                    content
            );
            temporaryObjects.add(inputBlobId);

            AsyncAnnotateFileRequest request = AsyncAnnotateFileRequest.newBuilder()
                    .addFeatures(Feature.newBuilder()
                            .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                            .build())
                    .setInputConfig(InputConfig.newBuilder()
                            .setMimeType(PDF_CONTENT_TYPE)
                            .setGcsSource(GcsSource.newBuilder()
                                    .setUri(gcsUri(ocrConfig.inputBucket(), inputObject))
                                    .build())
                            .build())
                    .setOutputConfig(OutputConfig.newBuilder()
                            .setBatchSize(Math.max(1, Math.min(ocrConfig.maxPdfPages(), 100)))
                            .setGcsDestination(GcsDestination.newBuilder()
                                    .setUri(gcsUri(ocrConfig.outputBucket(), outputPrefix))
                                    .build())
                            .build())
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                client.asyncBatchAnnotateFilesAsync(List.of(request))
                        .get(ocrConfig.timeoutSeconds(), TimeUnit.SECONDS);
            }

            return readPdfOutput(storage, outputPrefix, temporaryObjects);
        } catch (OcrProcessingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OcrProcessingException("PDF OCR 처리에 실패했습니다.", exception);
        } finally {
            deleteTemporaryObjects(storage, temporaryObjects);
        }
    }

    private String readPdfOutput(
            Storage storage,
            String outputPrefix,
            List<BlobId> temporaryObjects
    ) throws Exception {
        Page<Blob> page = storage.list(
                ocrConfig.outputBucket(),
                Storage.BlobListOption.prefix(outputPrefix)
        );
        List<Blob> outputs = new ArrayList<>();
        page.iterateAll().forEach(outputs::add);
        outputs.sort(Comparator.comparing(Blob::getName));

        StringBuilder text = new StringBuilder();
        int processedPages = 0;
        for (Blob output : outputs) {
            temporaryObjects.add(output.getBlobId());
            if (!output.getName().endsWith(".json")) {
                continue;
            }

            AnnotateFileResponse.Builder result = AnnotateFileResponse.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(
                    new String(output.getContent(), StandardCharsets.UTF_8),
                    result
            );
            for (AnnotateImageResponse response : result.getResponsesList()) {
                validateVisionResponse(response);
                processedPages++;
                if (processedPages > ocrConfig.maxPdfPages()) {
                    throw new OcrProcessingException(
                            "PDF는 최대 " + ocrConfig.maxPdfPages() + "페이지만 처리할 수 있습니다."
                    );
                }
                text.append(response.getFullTextAnnotation().getText()).append('\n');
            }
        }
        return requireText(text.toString());
    }

    private Storage createStorage() {
        if (isBlank(ocrConfig.projectId())) {
            return StorageOptions.getDefaultInstance().getService();
        }
        return StorageOptions.newBuilder()
                .setProjectId(ocrConfig.projectId())
                .build()
                .getService();
    }

    private void validateConfiguration(byte[] content) {
        if (!ocrConfig.enabled()) {
            throw new OcrProcessingException("OCR 기능이 비활성화되어 있습니다.");
        }
        if (content == null || content.length == 0
                || content.length > ocrConfig.maxFileSizeBytes()) {
            throw new OcrProcessingException("OCR 파일 크기가 허용 범위를 벗어났습니다.");
        }
    }

    private void validatePdfBuckets() {
        if (isBlank(ocrConfig.inputBucket()) || isBlank(ocrConfig.outputBucket())) {
            throw new OcrProcessingException(
                    "PDF OCR용 Google Cloud Storage 버킷 설정이 필요합니다."
            );
        }
    }

    private void validateVisionResponse(AnnotateImageResponse response) {
        if (response.hasError()) {
            throw new OcrProcessingException(
                    "Google Cloud Vision 오류: " + response.getError().getMessage()
            );
        }
    }

    private String requireText(String text) {
        if (isBlank(text)) {
            throw new OcrProcessingException("서류에서 읽을 수 있는 텍스트를 찾지 못했습니다.");
        }
        return text.trim();
    }

    private String gcsUri(String bucket, String object) {
        return "gs://" + bucket + "/" + object;
    }

    private void deleteTemporaryObjects(Storage storage, List<BlobId> blobIds) {
        for (BlobId blobId : blobIds) {
            try {
                storage.delete(blobId);
            } catch (RuntimeException ignored) {
                // OCR 성공 여부와 관계없이 임시 객체 정리를 시도하되 응답은 방해하지 않는다.
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
