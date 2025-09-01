package com.crushai.crushai.service;

import com.crushai.crushai.dto.AnalysisResponse;
import com.crushai.crushai.dto.GeminiRequest;
import com.crushai.crushai.dto.GeminiResponse;
import com.crushai.crushai.dto.MeaningfulWords;
import com.crushai.crushai.dto.SimplicityVsEffortMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.objectMapper = objectMapper;
    }

    // 기존 analyzeScreenshot 메서드 (변경 없음)
    public AnalysisResponse analyzeChatScreenshot(MultipartFile file, String userPrompt) throws IOException {
        String jsonSchemaPrompt = """
        Your analysis should be returned as a JSON object with the following structure:
        {
          "crushLevel": 1-100 (integer, prediction of other person's interest in the user),
          "meaningfulWords": {
            "user": ["word1", "word2", ...], // ONLY single impactful words (e.g., "curiosity", "future", "laugh") from the user's messages.
            "otherPerson": ["word1", "word2", ...] // ONLY single impactful words (e.g., "invite", "plans", "support") from the other person's messages.
          },
          "redFlags": ["Reason1", "Reason2", "Reason3"], // Up to 3 concise, high-level behavioral/emotional reasons, MAX 3-4 WORDS each (e.g., "low enthusiasm", "lack of curiosity", "generic responses"). Do NOT quote conversation content.
          "greenFlags": ["Reason1", "Reason2", "Reason3"], // Up to 3 concise, high-level behavioral/emotional reasons, MAX 3-4 WORDS each (e.g., "active engagement", "future planning", "personal sharing"). Do NOT quote conversation content.
          "nextMove": "A single, complete, and ready-to-copy-paste conversational prompt for the user to send next, based on the last message in the conversation to maintain flow. (e.g., 'Oh, what kind of soup are you thinking of making?', 'Sounds good! How about next Saturday?', 'I'm always up for trying new things, what's your favorite food?'). No explanations, just the prompt.",
          "vibeSummary": "A very concise 1-2 sentence factual summary of the overall conversation vibe (e.g., 'The conversation was generally playful and light-hearted.' or 'The tone was somewhat dry and focused on logistics.'). Focus on key facts, not detailed examples.",
          "simplicityVsEffort": {
            "avgMessageLengthWords": integer, // Average word count of the other person's messages.
            "questionRatio": double // Proportion of the other person's messages that contain a question (e.g., 0.1 for 10%). Value should be between 0.0 and 1.0.
          }
        }
        
        Analyze the chat screenshot based on the following:
        The right speech bubble is the user's message, and the left speech bubble is the other person's message.
        Based on the conversation content, rate how interested the other person is in the user from 1 to 100, and thoroughly analyze the reasons by quoting specific parts of the conversation. Ignore unnecessary parts of the conversation (time, date, read receipts, top bar, etc.) and focus only on the conversation content.
        Provide your analysis STRICTLY in the specified JSON format. Do NOT include any additional text outside the JSON block.
        """;
        // RestClient 호출 및 JSON 파싱 로직은 동일
        byte[] fileContent = file.getBytes();
        String encodedString = Base64.getEncoder().encodeToString(fileContent);

        GeminiRequest.Part textPart = new GeminiRequest.Part(jsonSchemaPrompt, null);
        GeminiRequest.Part imagePart = new GeminiRequest.Part(
                null,
                new GeminiRequest.InlineData(file.getContentType(), encodedString)
        );

        GeminiRequest.Content content = new GeminiRequest.Content(List.of(textPart, imagePart));
        GeminiRequest request = new GeminiRequest(Collections.singletonList(content));

        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                throw new IOException("Failed to get a valid response from Gemini API.");
            }

            String geminiTextResponse = response.getCandidates().get(0).getContent().getParts().get(0).getText();
            String jsonString = extractJson(geminiTextResponse);
            return objectMapper.readValue(jsonString, AnalysisResponse.class);

        } catch (Exception e) {
            System.err.println("Gemini API 호출 및 파싱 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to analyze chat screenshot: " + e.getMessage(), e);
        }
    }


    // --- 새로운 기능: nextMove 추천 ---
    public String generateNextMove(MultipartFile screenshotFile, int spicyLevel) throws IOException {
        String prompt;
        GeminiRequest request;

        if (screenshotFile != null && !screenshotFile.isEmpty()) {
            // Case 1: 이미지 파일이 있을 때 - 대화 분석 후 nextMove 추천
            byte[] fileContent = screenshotFile.getBytes();
            String encodedString = Base64.getEncoder().encodeToString(fileContent);
            String mimeType = screenshotFile.getContentType();

            // 이미지 있을 때 프롬프트 수정
            prompt = String.format(
                    """
                    Analyze the chat screenshot.
                    The right speech bubble is the user's message, and the left speech bubble is the other person's message.
                    Based on the conversation content, suggest a single, complete, and ready-to-copy-paste conversational prompt for the user to send next.
                    The prompt should match a 'spicy level' of %d (1 being very mild/polite, 10 being very bold/flirty).
                    For higher spicy levels (e.g., 7-10), incorporate popular Gen Z slang and trendy phrases naturally and appropriately.
                    Do NOT include any explanations, just the prompt text itself.
                    """,
                    spicyLevel
            );

            GeminiRequest.Part textPart = new GeminiRequest.Part(prompt, null);
            GeminiRequest.Part imagePart = new GeminiRequest.Part(
                    null,
                    new GeminiRequest.InlineData(mimeType, encodedString)
            );
            GeminiRequest.Content content = new GeminiRequest.Content(List.of(textPart, imagePart));
            request = new GeminiRequest(Collections.singletonList(content));

        } else {
            // Case 2: 이미지 파일이 없을 때 - 랜덤 픽업 라인 추천
            // 이미지 없을 때 프롬프트 수정
            prompt = String.format(
                    """
                    Generate a single, complete, and ready-to-copy-paste pickup line.
                    The pickup line should match a 'spicy level' of %d (1 being very mild/polite, 10 being very bold/flirty).
                    For higher spicy levels (e.g., 7-10), incorporate popular Gen Z slang and trendy phrases naturally and appropriately.
                    Do NOT include any explanations, just the pickup line text itself.
                    """,
                    spicyLevel
            );

            GeminiRequest.Part textPart = new GeminiRequest.Part(prompt, null);
            GeminiRequest.Content content = new GeminiRequest.Content(Collections.singletonList(textPart));
            request = new GeminiRequest(Collections.singletonList(content));
        }

        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                throw new IOException("Failed to get a valid response for next move from Gemini API.");
            }

            String geminiRawResponse = response.getCandidates().get(0).getContent().getParts().get(0).getText();

            // 마크다운 코드 블록 제거 및 추가적인 특수 문자 제거 (이전과 동일)
            String cleanedResponse = geminiRawResponse
                    .replace("```text", "").replace("```", "")
                    .replace("*", "").replace("_", "").replace("~", "")
                    .trim();

            return cleanedResponse;

        } catch (Exception e) {
            System.err.println("Gemini API 호출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to generate next move: " + e.getMessage(), e);
        }
    }

    private String extractJson(String text) {
        Pattern pattern = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            Pattern simpleJsonPattern = Pattern.compile("\\{[\\s\\S]*?\\}");
            Matcher simpleJsonMatcher = simpleJsonPattern.matcher(text);
            if (simpleJsonMatcher.find()) {
                return simpleJsonMatcher.group(0);
            } else {
                System.err.println("No JSON (neither markdown nor raw) found in Gemini response: " + text);
                return "{}";
            }
        }
    }
}