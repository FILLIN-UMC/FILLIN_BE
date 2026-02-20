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
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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
        이미지를 분석하여 다음 규칙에 따라 JSON으로 출력하세요.
        1. 아래 제공된 [PREDEFINED TITLE LIST]에서 이미지와 가장 잘 맞는 'title'을 선택하세요.
        2. 만약 리스트에 적합한 제목이 절대 없다면, 상황을 요약한 새로운 'title'을 직접 생성하세요. (10자 이내)
        3. 'category'는 반드시 [DANGER, INCONVENIENCE, DISCOVERY] 중 하나여야 합니다.
    
        --- PREDEFINED TITLE LIST ---
        %s
        -----------------------------
        출력 형식: {"title": "제목", "category": "카테고리"}
        마크다운 코드 블록 없이 순수 JSON만 출력하세요.
        """, this.predefinedLabelsText);

            Map<String, Object> requestBody = createRequestBody(textPrompt, base64Image);
            String apiUrl = geminiUrl + apiKey;

            // GCV 분석 데이터 버퍼 용량 10MB로 확장 (256KB 에러 완벽 방지)
            ExchangeStrategies strategies = ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build();

            String responseBody = webClientBuilder.build()
                    .mutate()
                    .exchangeStrategies(strategies)
                    .build()
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

            // 이미지 크기를 미리 확인 (얼굴 좌표 정규화에 필요)
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) throw new IOException("이미지 디코딩 실패");
            int width = image.getWidth();
            int height = image.getHeight();

            // GCV 호출: 번호판과 얼굴 좌표 리스트 가져오기
            List<BoundingBox> boxes = detectSensitiveAreasWithGcv(imageBytes, width, height);

            if (boxes.isEmpty()) return ReportImageProcessResponse.notDetected();

            // 모자이크 적용 및 S3 업로드
            MultipartFile processedImage = applyMosaicWithImage(image, boxes, imageFile);
            String imageUrl = s3Service.uploadImage(processedImage);

            return ReportImageProcessResponse.detected(imageUrl);

        } catch (Exception e) {
            log.error("GCV 스마트 처리 중 오류: {}", e.getMessage());
            return ReportImageProcessResponse.notDetected();
        }
    }

    private List<BoundingBox> detectSensitiveAreasWithGcv(byte[] imageBytes, int width, int height) throws IOException {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> request = Map.of(
                "requests", List.of(Map.of(
                        "image", Map.of("content", base64Image),
                        "features", List.of(
                                Map.of("type", "OBJECT_LOCALIZATION"), // 번호판(사물)
                                Map.of("type", "FACE_DETECTION"),       // 얼굴
                                Map.of("type", "TEXT_DETECTION")         // 번호판 숫자(글자) -> 추가!
                        )
                ))
        );
        // GCV 분석 데이터 버퍼 용량 10MB로 확장 (256KB 에러 완벽 방지)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        String responseBody = webClientBuilder.build()
                .mutate()
                .exchangeStrategies(strategies)
                .build()
                .post()
                .uri(GCV_URL + gcvApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseGcvResponse(responseBody, width, height);
    }

    // 1. GCV 응답 파싱 및 영역 필터링 로직
    private List<BoundingBox> parseGcvResponse(String responseBody, int width, int height) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode response = root.path("responses").get(0);
            List<BoundingBox> boxes = new ArrayList<>();
            List<BoundingBox> carBoxes = new ArrayList<>(); // 자동차 영역 저장용

            // [Step 1] 객체 감지 파싱 (번호판은 즉시 추가, 자동차는 carBoxes에 저장)
            JsonNode objectAnnotations = response.path("localizedObjectAnnotations");
            if (objectAnnotations.isArray()) {
                for (JsonNode node : objectAnnotations) {
                    String name = node.path("name").asText();
                    JsonNode vertices = node.path("boundingPoly").path("normalizedVertices");

                    if ("Car".equalsIgnoreCase(name)) {
                        carBoxes.add(extractNormalizedBox(vertices)); // 자동차 위치 파악
                    } else if ("License plate".equalsIgnoreCase(name) || "Vehicle registration plate".equalsIgnoreCase(name)) {
                        boxes.add(extractNormalizedBox(vertices)); // 번호판은 무조건 추가
                    }
                }
            }

            // [Step 2] 얼굴 감지 파싱 (얼굴은 무조건 추가)
            JsonNode faceAnnotations = response.path("faceAnnotations");
            if (faceAnnotations.isArray()) {
                for (JsonNode node : faceAnnotations) {
                    boxes.add(extractPixelBox(node.path("boundingPoly").path("vertices"), width, height));
                }
            }

            // [Step 3] 텍스트 감지 (자동차 영역 안에 있는 텍스트만 선별 추가)
            JsonNode textAnnotations = response.path("textAnnotations");
            if (textAnnotations.isArray() && textAnnotations.size() > 1) {
                for (int i = 1; i < textAnnotations.size(); i++) {
                    JsonNode textNode = textAnnotations.get(i);
                    String detectedText = textNode.path("description").asText(); // 감지된 글자 내용
                    BoundingBox textBox = extractPixelBox(textNode.path("boundingPoly").path("vertices"), width, height);

                    // 1. 숫자가 포함되어 있고 2. 자동차 영역(5% 축소) 내부에 있는 경우만!
                    if (detectedText.matches(".*[0-9].*") && isStrictlyInsideCar(textBox, carBoxes)) {
                        boxes.add(expandBox(textBox, 20, 10));
                    }
                }
            }
            return boxes;
        } catch (Exception e) {
            log.error("GCV 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // 2. 텍스트가 자동차 영역 내부에 있는지 검증하는 헬퍼 메서드
    private boolean isStrictlyInsideCar(BoundingBox text, List<BoundingBox> cars) {
        for (BoundingBox car : cars) {
            // 가로 세로 5%씩 안쪽으로 좁힌 경계선 계산
            int marginX = (car.xmax() - car.xmin()) * 5 / 100;
            int marginY = (car.ymax() - car.ymin()) * 5 / 100;

            int centerX = (text.xmin() + text.xmax()) / 2;
            int centerY = (text.ymin() + text.ymax()) / 2;

            if (centerX >= (car.xmin() + marginX) && centerX <= (car.xmax() - marginX) &&
                    centerY >= (car.ymin() + marginY) && centerY <= (car.ymax() - marginY)) {
                return true;
            }
        }
        return false;
    }

    // 3. 정규화 좌표(0.0~1.0) 추출 헬퍼
    private BoundingBox extractNormalizedBox(JsonNode vertices) {
        return new BoundingBox(
                (int)(vertices.get(0).path("y").asDouble(0) * 1000),
                (int)(vertices.get(0).path("x").asDouble(0) * 1000),
                (int)(vertices.get(2).path("y").asDouble(0) * 1000),
                (int)(vertices.get(2).path("x").asDouble(0) * 1000)
        );
    }

    // 4. 픽셀 좌표 추출 및 정규화(0~1000) 헬퍼
    private BoundingBox extractPixelBox(JsonNode vertices, int width, int height) {
        int xmin = vertices.get(0).path("x").asInt(0);
        int ymin = vertices.get(0).path("y").asInt(0);
        int xmax = vertices.get(2).path("x").asInt(width);
        int ymax = vertices.get(2).path("y").asInt(height);

        return new BoundingBox(
                (ymin * 1000) / height, (xmin * 1000) / width,
                (ymax * 1000) / height, (xmax * 1000) / width
        );
    }

    // 5. 박스 크기 확장 헬퍼 (텍스트 박스 빈틈 메우기 용)
    private BoundingBox expandBox(BoundingBox box, int padX, int padY) {
        return new BoundingBox(
                Math.max(0, box.ymin() - padY), Math.max(0, box.xmin() - padX),
                Math.min(1000, box.ymax() + padY), Math.min(1000, box.xmax() + padX)
        );
    }

    private MultipartFile applyMosaicWithImage(BufferedImage image, List<BoundingBox> boxes, MultipartFile originalFile) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        int verticalPadding = 10;

        for (BoundingBox box : boxes) {
            int paddedYmin = Math.max(0, box.ymin() - verticalPadding);
            int paddedYmax = Math.min(1000, box.ymax() + verticalPadding);

            int x = (box.xmin() * width) / 1000;
            int y = (paddedYmin * height) / 1000;
            int w = ((box.xmax() - box.xmin()) * width) / 1000;
            int h = ((paddedYmax - paddedYmin) * height) / 1000;

            x = Math.max(0, x); y = Math.max(0, y);
            w = Math.min(width - x, w); h = Math.min(height - y, h);

            mosaicArea(image, x, y, w, h);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String extension = getExtension(originalFile.getOriginalFilename());
        ImageIO.write(image, extension.isEmpty() ? "jpg" : extension, baos);

        return new CustomMultipartFile(
                baos.toByteArray(), originalFile.getName(),
                originalFile.getOriginalFilename(), originalFile.getContentType()
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
        String categoryStr = jsonResult.path("category").asText();

        ReportCategory category;
        // 1단계: 서버의 매핑 테이블에서 먼저 카테고리 확인 (일관성 유지)
        if (titleCategoryMap.containsKey(title)) {
            category = titleCategoryMap.get(title);
        } else {
            // 2단계: 리스트에 없는 제목인 경우, AI가 제안한 카테고리 사용 (유연성 확보)
            try {
                category = ReportCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                category = ReportCategory.DISCOVERY; // 최후의 기본값
            }
        }

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