package com.fillin.service.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.dto.report.response.ReportAnalysisResponseDto;
import com.fillin.dto.report.response.ReportImageProcessResponse;
import com.fillin.service.s3.S3Service;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAnalysisService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.url}")
    private String geminiUrl;

    @Value("${gcv.api-key}")
    private String gcvApiKey;

    private final String GCV_URL = "https://vision.googleapis.com/v1/images:annotate?key=";

    @Value("classpath:prompts/report-labels.txt")
    private Resource labelResource;

    private String predefinedLabelsText;
    private final Map<String, ReportCategory> titleCategoryMap = new HashMap<>();

    @PostConstruct
    public void init() {
        try (InputStream inputStream = labelResource.getInputStream()) {
            this.predefinedLabelsText = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            parseLabelsToMap(this.predefinedLabelsText);
            log.info("Report resources (labels & example image) loaded successfully.");
        } catch (IOException e) {
            log.error("Failed to load report labels", e);
            throw new RuntimeException("리포트 라벨 파일을 불러오는 데 실패했습니다.");
        }
    }

    private void parseLabelsToMap(String text) {
        String[] lines = text.split("\n");
        ReportCategory currentCategory = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                String categoryStr = line.substring(1, line.length() - 1);
                try {
                    currentCategory = ReportCategory.valueOf(categoryStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown category in label file: {}", categoryStr);
                    currentCategory = null;
                }
            } else if (currentCategory != null) {
                String[] titles = line.split(",");
                for (String title : titles) {
                    titleCategoryMap.put(title.trim(), currentCategory);
                }
            }
        }
    }

    // 이미지 카테고리 및 제목 분석
    public ReportAnalysisResponseDto analyzeImage(MultipartFile imageFile) {
        try {
            byte[] imageBytes = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String textPrompt = String.format("""
                Analyze this image and provide a JSON output.
                You MUST select the 'title' strictly from the predefined list below.
                Only provide the 'title' field in JSON.
                --- PREDEFINED TITLE LIST ---
                %s
                -----------------------------
                DO NOT output markdown code blocks. Just raw JSON.
                """, this.predefinedLabelsText);

            Map<String, Object> requestBody = createRequestBody(textPrompt, base64Image);
            String apiUrl = geminiUrl + apiKey;

            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(responseBody);

        } catch (IOException e) {
            log.error("Image processing error", e);
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Gemini API call error", e);
            throw new RuntimeException("AI 분석 중 오류가 발생했습니다.");
        }
    }

    // 번호판 등 감지
    public ReportImageProcessResponse processImageSmart(MultipartFile imageFile) {
        try {
            byte[] imageBytes = imageFile.getBytes();

            // GCV 호출하여 번호판 좌표 리스트 바로 가져오기
            List<BoundingBox> boxes = detectLicensePlateWithGcv(imageBytes);

            if (boxes.isEmpty()) return ReportImageProcessResponse.notDetected();

            // 기존 모자이크 및 S3 업로드 로직 유지
            MultipartFile processedImage = applyMosaic(imageBytes, boxes, imageFile);
            String imageUrl = s3Service.uploadImage(processedImage);

            return ReportImageProcessResponse.detected(imageUrl);

        } catch (Exception e) {
            log.error("GCV 스마트 처리 중 오류: {}", e.getMessage());
            return ReportImageProcessResponse.notDetected();
        }
    }

    private List<BoundingBox> detectLicensePlateWithGcv(byte[] imageBytes) throws IOException {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // GCV Object Localization 요청 바디 구성
        Map<String, Object> request = Map.of(
                "requests", List.of(Map.of(
                        "image", Map.of("content", base64Image),
                        "features", List.of(Map.of("type", "OBJECT_LOCALIZATION"))
                ))
        );

        String responseBody = webClientBuilder.build()
                .post()
                .uri(GCV_URL + gcvApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseGcvResponse(responseBody);
    }

    private List<BoundingBox> parseGcvResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode annotations = root.path("responses").get(0).path("localizedObjectAnnotations");

            List<BoundingBox> boxes = new ArrayList<>();
            if (annotations.isArray()) {
                for (JsonNode node : annotations) {
                    String name = node.path("name").asText();
                    // 'License plate' 객체만 필터링
                    if ("License plate".equalsIgnoreCase(name)) {
                        JsonNode vertices = node.path("boundingPoly").path("normalizedVertices");

                        // GCV는 0.0~1.0 좌표를 주므로 기존 로직(0~1000)에 맞춰 변환
                        float xmin = (float) vertices.get(0).path("x").asDouble(0);
                        float ymin = (float) vertices.get(0).path("y").asDouble(0);
                        float xmax = (float) vertices.get(2).path("x").asDouble(0);
                        float ymax = (float) vertices.get(2).path("y").asDouble(0);

                        boxes.add(new BoundingBox(
                                (int)(ymin * 1000), (int)(xmin * 1000),
                                (int)(ymax * 1000), (int)(xmax * 1000)
                        ));
                    }
                }
            }
            return boxes;
        } catch (Exception e) {
            log.error("GCV 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private MultipartFile applyMosaic(byte[] imageBytes, List<BoundingBox> boxes, MultipartFile originalFile) throws IOException {
        // 1. 바이트 배열로부터 이미지 로드 (null 체크 포함)
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) throw new IOException("이미지 파일을 디코딩할 수 없습니다.");

        int width = image.getWidth();
        int height = image.getHeight();

        // 상하로 확장할 패딩 값 (0-1000 좌표계 기준)
        int verticalPadding = 10;

        for (BoundingBox box : boxes) {
            // [수정] 상하 범위를 10씩 확장 (이미지 경계 0~1000 준수)
            int paddedYmin = Math.max(0, box.ymin() - verticalPadding);
            int paddedYmax = Math.min(1000, box.ymax() + verticalPadding);

            // 2. 확장된 좌표를 실제 픽셀 좌표로 변환
            int x = (box.xmin() * width) / 1000;
            int y = (paddedYmin * height) / 1000;
            int w = ((box.xmax() - box.xmin()) * width) / 1000;
            int h = ((paddedYmax - paddedYmin) * height) / 1000;

            // 3. 픽셀 레벨 좌표 보정 (이미지 크기 초과 방지)
            x = Math.max(0, x);
            y = Math.max(0, y);
            w = Math.min(width - x, w);
            h = Math.min(height - y, h);

            // 4. 모자이크 적용
            mosaicArea(image, x, y, w, h);
        }

        // 5. 결과 이미지 처리 및 MultipartFile 생성
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String extension = getExtension(originalFile.getOriginalFilename());
        ImageIO.write(image, extension.isEmpty() ? "jpg" : extension, baos);

        return new CustomMultipartFile(
                baos.toByteArray(),
                originalFile.getName(),
                originalFile.getOriginalFilename(),
                originalFile.getContentType()
        );
    }

    private void mosaicArea(BufferedImage image, int x, int y, int w, int h) {
        int mosaicSize = 15;
        for (int i = x; i < x + w; i += mosaicSize) {
            for (int j = y; j < y + h; j += mosaicSize) {
                int bw = Math.min(mosaicSize, x + w - i);
                int bh = Math.min(mosaicSize, y + h - j);
                int pixelColor = image.getRGB(i, j);
                for (int m = 0; m < bw; m++) {
                    for (int n = 0; n < bh; n++) {
                        image.setRGB(i + m, j + n, pixelColor);
                    }
                }
            }
        }
    }

    private Map<String, Object> createRequestBody(String textPrompt, String base64Image) {
        Map<String, Object> partText = Map.of("text", textPrompt);
        Map<String, Object> inlineData = Map.of("mime_type", "image/jpeg", "data", base64Image);
        Map<String, Object> partImage = Map.of("inline_data", inlineData);
        return Map.of("contents", List.of(Map.of("parts", List.of(partText, partImage))));
    }

    private ReportAnalysisResponseDto parseGeminiResponse(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String textResult = getTextFromCandidate(rootNode);
        textResult = textResult.replace("```json", "").replace("```", "").trim();
        JsonNode jsonResult = objectMapper.readTree(textResult);
        String title = jsonResult.path("title").asText("분석 불가");
        ReportCategory category = titleCategoryMap.getOrDefault(title, ReportCategory.DISCOVERY);
        return new ReportAnalysisResponseDto(title, category);
    }

    private String getTextFromCandidate(JsonNode rootNode) {
        return rootNode.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();
    }

    private String getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1))
                .orElse("");
    }

    private record BoundingBox(int ymin, int xmin, int ymax, int xmax) {}

    static class CustomMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String originalFilename;
        private final String contentType;

        public CustomMultipartFile(byte[] content, String name, String originalFilename, String contentType) {
            this.content = content;
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() throws IOException { return content; }
        @Override public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) { fos.write(content); }
        }
    }
}