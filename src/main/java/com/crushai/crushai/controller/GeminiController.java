package com.crushai.crushai.controller;

import com.crushai.crushai.dto.AnalysisResponse; // 기존 응답 DTO (이번 기능에는 사용 안 함)
import com.crushai.crushai.service.GeminiService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Spring MVC의 MultipartFile 임포트

import java.io.IOException;

@RestController
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    // 기존 분석 기능 (변경 없음)
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> analyzeScreenshot(
            @RequestPart("screenshot") MultipartFile screenshotFile) {

        System.out.println("메시지 분석 시작!");

        String prompt = "이 이미지는 메신저 대화 내용 캡쳐본입니다. 오른쪽 말풍선은 사용자의 메시지이고, 왼쪽 말풍선은 상대방의 메시지입니다. 대화 내용을 바탕으로 상대방이 사용자에게 얼마나 관심이 있는지 1점부터 100점까지 점수를 매기고, 그 이유를 대화의 구체적인 부분을 인용하여 자세히 분석해주세요. 대화 내용 중 불필요한 부분(시간, 날짜, 읽음 표시, 상단바 등)은 무시하고 대화 내용만 집중해주세요.";

        if (screenshotFile.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            AnalysisResponse analysisResult = geminiService.analyzeChatScreenshot(screenshotFile, prompt);
            System.out.println("메시지 분석 완료!");
            return ResponseEntity.ok(analysisResult);
        } catch (IOException e) {
            System.out.println("메시지 분석 실패!");
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        } catch (Exception e) {
            System.out.println("메시지 분석 실패!");
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // --- 두 번째 기능: nextMove 추천 ---
    @PostMapping(value = "/next-move", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> getNextMoveRecommendation(
            @RequestParam("spicyLevel") int spicyLevel, // 매운맛 레벨 (필수)
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshotFile) { // 스크린샷 (선택적)

        if (spicyLevel < 1 || spicyLevel > 100) {
            return ResponseEntity.badRequest().body("Spicy level must be between 1 and 10.");
        }

        try {
            String nextMove = geminiService.generateNextMove(screenshotFile, spicyLevel);
            return ResponseEntity.ok(nextMove);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error processing file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error generating next move: " + e.getMessage());
        }
    }
}