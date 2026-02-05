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

    @Value("classpath:prompts/report-labels.txt")
    private Resource labelResource;

    private String predefinedLabelsText;
    private final Map<String, ReportCategory> titleCategoryMap = new HashMap<>();

    // One-shot Learning용 변수
    private String exampleImageBase64;
    private final String EXAMPLE_COORDINATES = "{\"licensePlates\": [{\"ymin\": 435, \"xmin\": 381, \"ymax\": 536, \"xmax\": 646}]}";

    private String example2ImageBase64;
    private final String EXAMPLE2_COORDINATES = "{\"licensePlates\": [{\"ymin\": 650, \"xmin\": 406, \"ymax\": 708, \"xmax\": 512}]}";

    @PostConstruct
    public void init() {
        try (InputStream inputStream = labelResource.getInputStream()) {
            this.predefinedLabelsText = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            parseLabelsToMap(this.predefinedLabelsText);

            loadExampleImages();

            log.info("Report resources (labels & example image) loaded successfully.");
        } catch (IOException e) {
            log.error("Failed to load report labels", e);
            throw new RuntimeException("리포트 라벨 파일을 불러오는 데 실패했습니다.");
        }
    }

    private void loadExampleImages() {
        try {
            Resource res1 = new org.springframework.core.io.ClassPathResource("examples/_image_3.jpg");
            this.exampleImageBase64 = Base64.getEncoder().encodeToString(StreamUtils.copyToByteArray(res1.getInputStream()));

            Resource res2 = new org.springframework.core.io.ClassPathResource("examples/_image_5.jpg");
            this.example2ImageBase64 = Base64.getEncoder().encodeToString(StreamUtils.copyToByteArray(res2.getInputStream()));
        } catch (IOException e) {
            log.error("Failed to load example images from classpath", e);
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

    // =================================================================================
    // [신규 기능] 스마트 번호판 감지 및 모자이크 (Fast Check -> Process)
    // =================================================================================
    public ReportImageProcessResponse processImageSmart(MultipartFile imageFile) {
        try {
            byte[] imageBytes = imageFile.getBytes();
            boolean hasPlate = checkLicensePlateExistence(imageBytes);

            if (!hasPlate) return ReportImageProcessResponse.notDetected();

            List<BoundingBox> boxes = detectLicensePlateCoordinates(imageBytes);
            if (boxes.isEmpty()) return ReportImageProcessResponse.notDetected();

            MultipartFile processedImage = applyMosaic(imageBytes, boxes, imageFile);
            String imageUrl = s3Service.uploadImage(processedImage);

            return ReportImageProcessResponse.detected(imageUrl);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
            // [수정] 429 에러 발생 시 로그를 남기고 커스텀 예외를 던짐
            log.error("Gemini API 할당량 초과: {}", e.getMessage());
            throw new RuntimeException("재미나이 API 호출 한도를 초과했습니다. 1분 뒤에 다시 시도해주세요.");

        } catch (Exception e) {
            log.error("스마트 이미지 처리 중 일반 오류: {}", e.getMessage());
            return ReportImageProcessResponse.notDetected();
        }
    }

    private boolean checkLicensePlateExistence(byte[] imageBytes) throws IOException {
        String prompt = """
            Is there a vehicle license plate (car registration plate) clearly visible in this image?
            Answer strictly with 'true' or 'false'.
            Do not include any other text.
            """;

        String response = callGemini(imageBytes, prompt);
        return parseBooleanResponse(response);
    }

    private List<BoundingBox> detectLicensePlateCoordinates(byte[] imageBytes) throws IOException {
        String prompt = """
        이미지에서 모든 자동차 번호판을 찾아내고 좌표를 추출해줘.
        
        [출력 형식 및 키 이름 엄수]
        반드시 아래의 키 이름을 가진 JSON 객체로만 응답해야 해. 키 이름을 절대 바꾸지 마.
        - "ymin": 번호판의 상단 좌표 (0-1000)
        - "xmin": 번호판의 왼쪽 좌표 (0-1000)
        - "ymax": 번호판의 하단 좌표 (0-1000)
        - "xmax": 번호판의 오른쪽 좌표 (0-1000)
        
        [JSON 응답 예시]
        {"licensePlates": [{"ymin": 435, "xmin": 381, "ymax": 536, "xmax": 646}]}
        
        [주의사항]
        1. "x", "max_x", "y_min" 같은 다른 키 이름을 사용하면 절대 안 돼. 반드시 위의 4개 키만 사용해.
        2. 번호판이 없으면 {"licensePlates": []} 라고 응답해.
        3. 마크다운 코드 블록 없이 순수 JSON으로만 출력해줘.
        """;

        String response = callGemini(imageBytes, prompt);
        log.info("[Gemini Raw Response]: {}", response);
        return parseBoundingBoxes(response);
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

    // =================================================================================
    // [공통 유틸] Gemini 호출 및 파싱
    // =================================================================================
    private String callGemini(byte[] imageBytes, String prompt) throws IOException {
        String apiUrl = geminiUrl + apiKey;
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // One-shot Learning 적용
        Map<String, Object> requestBody = createFewShotRequestBody(prompt, base64Image);

        return webClientBuilder.build()
                .post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private Map<String, Object> createRequestBody(String textPrompt, String base64Image) {
        Map<String, Object> partText = Map.of("text", textPrompt);
        Map<String, Object> inlineData = Map.of("mime_type", "image/jpeg", "data", base64Image);
        Map<String, Object> partImage = Map.of("inline_data", inlineData);
        return Map.of("contents", List.of(Map.of("parts", List.of(partText, partImage))));
    }

    private Map<String, Object> createFewShotRequestBody(String textPrompt, String targetBase64Image) {
        List<Map<String, Object>> parts = new ArrayList<>();

        // 예시 1 (BMW)
        parts.add(Map.of("inline_data", Map.of("mime_type", "image/jpeg", "data", exampleImageBase64)));
        parts.add(Map.of("text", "예시 1: 차량 번호판의 표준 위치와 모양이야. " + EXAMPLE_COORDINATES));

        // 예시 2 (그랜저) - 그림자 및 그릴 오답 방지 강조
        parts.add(Map.of("inline_data", Map.of("mime_type", "image/jpeg", "data", example2ImageBase64)));
        parts.add(Map.of("text", """
        예시 2: 이 차량은 번호판 아래에 검은색 그릴이 있고, 바닥에 어두운 그림자가 있어.
        [필독 지시사항]
        1. 절대로 도로 바닥에 생긴 '검은색 그림자'를 번호판으로 착각하지 마.
        2. 차량 범퍼에 붙어 있는 '흰색 바탕의 숫자판'만 골라야 해.
        3. 차 색상이 무엇이든 관계없이 오직 흰색 번호판 영역만 집중해.
        정답 좌표: """ + EXAMPLE2_COORDINATES));

        // 본 요청
        parts.add(Map.of("text", "위의 지침을 완벽히 숙지해서, 이 새로운 사진에서도 '그림자'가 아닌 '진짜 번호판'만 찾아줘: " + textPrompt));
        parts.add(Map.of("inline_data", Map.of("mime_type", "image/jpeg", "data", targetBase64Image)));

        return Map.of("contents", List.of(Map.of("parts", parts)));
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
            return text.contains("true");
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
                    // 다양한 키 이름 변종에 대응 (방어적 파싱)
                    int ymin = node.has("ymin") ? node.get("ymin").asInt() : node.path("y_min").asInt(0);
                    int xmin = node.has("xmin") ? node.get("xmin").asInt() : node.path("x").asInt(node.path("x_min").asInt(0));
                    int ymax = node.has("ymax") ? node.get("ymax").asInt() : node.path("y_max").asInt(ymin + 50); // ymax 없으면 임의 확장
                    int xmax = node.has("xmax") ? node.get("xmax").asInt() : node.path("max_x").asInt(node.path("x_max").asInt(0));

                    boxes.add(new BoundingBox(ymin, xmin, ymax, xmax));
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