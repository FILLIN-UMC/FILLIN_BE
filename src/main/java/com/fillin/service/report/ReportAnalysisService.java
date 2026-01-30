package com.fillin.service.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.dto.report.response.ReportAnalysisResponseDto;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAnalysisService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

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

    public ReportAnalysisResponseDto analyzeImage(MultipartFile imageFile) {
        String apiUrl = geminiUrl + apiKey;

        try {
            byte[] imageBytes = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            Map<String, Object> requestBody = createRequestBody(base64Image);

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

    private Map<String, Object> createRequestBody(String base64Image) {
        String textPrompt = String.format("""
                Analyze this image and provide a JSON output.
                
                You MUST select the 'title' strictly from the predefined list below.
                Do NOT create a new title.
                Only provide the 'title' field in JSON.
                
                --- PREDEFINED TITLE LIST ---
                %s
                -----------------------------
                
                Example Output:
                {"title": "도로 파손 심함"}
                
                DO NOT output markdown code blocks. Just raw JSON.
                """, this.predefinedLabelsText);

        Map<String, Object> partText = Map.of("text", textPrompt);
        Map<String, Object> inlineData = Map.of("mime_type", "image/jpeg", "data", base64Image);
        Map<String, Object> partImage = Map.of("inline_data", inlineData);
        Map<String, Object> content = Map.of("parts", List.of(partText, partImage));

        return Map.of("contents", List.of(content));
    }

    private ReportAnalysisResponseDto parseGeminiResponse(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String textResult = rootNode.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        textResult = textResult.replace("```json", "").replace("```", "").trim();

        JsonNode jsonResult = objectMapper.readTree(textResult);
        String title = jsonResult.path("title").asText("분석 불가");

        ReportCategory category = titleCategoryMap.getOrDefault(title, ReportCategory.DISCOVERY);

        return new ReportAnalysisResponseDto(title, category);
    }
}