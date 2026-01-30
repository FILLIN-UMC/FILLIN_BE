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
    private final S3Service s3Service; // [추가] S3 업로드용

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.url}")
    private String geminiUrl;

    @Value("classpath:prompts/report-labels.txt")
    private Resource labelResource;

    private String predefinedLabelsText;

    private final Map<String, ReportCategory> titleCategoryMap = new HashMap<>();

    @PostConstruct
    public void init() {
        try (InputStream inputStream = labelResource.getInputStream()) {
            this.predefinedLabelsText = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            parseLabelsToMap(this.predefinedLabelsText);
            log.info("Report labels loaded successfully. Total mappings: {}", titleCategoryMap.size());
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

    // =================================================================================
    // [기존 기능] 이미지 제목/카테고리 분석
    // =================================================================================
    public ReportAnalysisResponseDto analyzeImage(MultipartFile imageFile) {
        String apiUrl = geminiUrl + apiKey;

        try {
            byte[] imageBytes = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 분석용 프롬프트 생성
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

    // =================================================================================
    // [신규 기능] 스마트 번호판 감지 및 모자이크 (Fast Check -> Process)
    // =================================================================================
    public ReportImageProcessResponse processImageSmart(MultipartFile imageFile) {
        try {
            // 1. 번호판 존재 여부 확인 (Light Check)
            boolean hasPlate = checkLicensePlateExistence(imageFile);

            if (!hasPlate) {
                // [Case 1] 번호판 없음 -> 즉시 반환 (프론트가 원본 업로드)
                return ReportImageProcessResponse.notDetected();
            }

            // [Case 2] 번호판 있음 -> 좌표 추출 -> 모자이크 -> S3 업로드
            List<BoundingBox> boxes = detectLicensePlateCoordinates(imageFile);

            if (boxes.isEmpty()) {
                return ReportImageProcessResponse.notDetected(); // 좌표 못 찾으면 원본 사용
            }

            MultipartFile processedImage = applyMosaic(imageFile, boxes);
            String imageUrl = s3Service.uploadImage(processedImage);

            return ReportImageProcessResponse.detected(imageUrl);

        } catch (Exception e) {
            log.error("스마트 이미지 처리 중 오류 (원본 사용 권장): {}", e.getMessage());
            return ReportImageProcessResponse.notDetected();
        }
    }

    // [Step 1] 존재 여부 확인 (Boolean Return)
    private boolean checkLicensePlateExistence(MultipartFile imageFile) throws IOException {
        String prompt = """
            Is there a vehicle license plate (car registration plate) clearly visible in this image?
            Answer strictly with 'true' or 'false'.
            Do not include any other text.
            """;

        String response = callGemini(imageFile, prompt);
        return parseBooleanResponse(response);
    }

    // [Step 2] 좌표 추출 (JSON Return)
    private List<BoundingBox> detectLicensePlateCoordinates(MultipartFile imageFile) throws IOException {
        String prompt = """
            Detect all license plates in this image.
            Return a JSON object with a key "licensePlates" containing a list of bounding boxes.
            Each box must have "ymin", "xmin", "ymax", "xmax" (integer 0-1000 normalized coordinates).
            Example: {"licensePlates": [{"ymin": 500, "xmin": 200, "ymax": 600, "xmax": 400}]}
            If none, return {"licensePlates": []}.
            Just raw JSON.
            """;

        String response = callGemini(imageFile, prompt);
        return parseBoundingBoxes(response);
    }

    // [Step 3] 모자이크 적용
    private MultipartFile applyMosaic(MultipartFile originalFile, List<BoundingBox> boxes) throws IOException {
        BufferedImage image = ImageIO.read(originalFile.getInputStream());
        int width = image.getWidth();
        int height = image.getHeight();

        for (BoundingBox box : boxes) {
            int x = (box.xmin() * width) / 1000;
            int y = (box.ymin() * height) / 1000;
            int w = ((box.xmax() - box.xmin()) * width) / 1000;
            int h = ((box.ymax() - box.ymin()) * height) / 1000;

            // 좌표 보정
            x = Math.max(0, x);
            y = Math.max(0, y);
            w = Math.min(width - x, w);
            h = Math.min(height - y, h);

            mosaicArea(image, x, y, w, h);
        }

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
        int mosaicSize = 15; // 모자이크 입자 크기
        for (int i = x; i < x + w; i += mosaicSize) {
            for (int j = y; j < y + h; j += mosaicSize) {
                int bw = Math.min(mosaicSize, x + w - i);
                int bh = Math.min(mosaicSize, y + h - j);
                int pixelColor = image.getRGB(i, j); // 좌상단 색상으로 채움
                for (int m = 0; m < bw; m++) {
                    for (int n = 0; n < bh; n++) {
                        image.setRGB(i + m, j + n, pixelColor);
                    }
                }
            }
        }
    }

    // =================================================================================
    // [공통 유틸] Gemini 호출 및 파싱
    // =================================================================================
    private String callGemini(MultipartFile imageFile, String prompt) throws IOException {
        String apiUrl = geminiUrl + apiKey;
        byte[] imageBytes = imageFile.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        Map<String, Object> requestBody = createRequestBody(prompt, base64Image);

        return webClientBuilder.build()
                .post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // 기존 createRequestBody 리팩토링 (프롬프트 파라미터화)
    private Map<String, Object> createRequestBody(String textPrompt, String base64Image) {
        Map<String, Object> partText = Map.of("text", textPrompt);
        Map<String, Object> inlineData = Map.of("mime_type", "image/jpeg", "data", base64Image);
        Map<String, Object> partImage = Map.of("inline_data", inlineData);
        Map<String, Object> content = Map.of("parts", List.of(partText, partImage));
        return Map.of("contents", List.of(content));
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

    private boolean parseBooleanResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = getTextFromCandidate(root).trim().toLowerCase();
            return Boolean.parseBoolean(text);
        } catch (Exception e) {
            return false;
        }
    }

    private List<BoundingBox> parseBoundingBoxes(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = getTextFromCandidate(root).replace("```json", "").replace("```", "").trim();

            JsonNode jsonNode = objectMapper.readTree(text);
            JsonNode listNode = jsonNode.path("licensePlates");

            List<BoundingBox> boxes = new ArrayList<>();
            if (listNode.isArray()) {
                for (JsonNode node : listNode) {
                    boxes.add(new BoundingBox(
                            node.path("ymin").asInt(),
                            node.path("xmin").asInt(),
                            node.path("ymax").asInt(),
                            node.path("xmax").asInt()
                    ));
                }
            }
            return boxes;
        } catch (Exception e) {
            log.error("좌표 파싱 실패: {}", e.getMessage());
            return List.of();
        }
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

    // 내부 DTO 및 클래스
    private record BoundingBox(int ymin, int xmin, int ymax, int xmax) {}

    // MultipartFile 구현체 (BufferedImage -> MultipartFile 변환용)
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