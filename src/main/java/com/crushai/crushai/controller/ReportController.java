package com.crushai.crushai.controller;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.ReportResponse;
import com.crushai.crushai.dto.UserReportDto;
import org.springframework.http.HttpStatus;
import com.crushai.crushai.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 신고 관련 API 컨트롤러
 * 사용자 신고, 채팅 신고 등을 처리
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * 사용자 신고
     * POST /api/report/user
     *
     * Request Body:
     * {
     *   "reportedUserId": 123,
     *   "reportType": "FAKE" // or "SEXUAL"
     * }
     *
     * Response (Success - 200):
     * {
     *   "success": true,
     *   "message": "신고가 접수되었습니다"
     * }
     *
     * Response (Error - 400):
     * {
     *   "success": false,
     *   "message": "자기 자신을 신고할 수 없습니다"
     * }
     *
     * Response (Error - 409):
     * {
     *   "success": false,
     *   "message": "이미 신고한 사용자입니다"
     * }
     */
    @PostMapping("/user")
    public ResponseEntity<ReportResponse> reportUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserReportDto userReportDto
    ) {
        Long reporterId = userDetails.getUserId();
        log.info("User {} reporting user {} for {}",
                reporterId, userReportDto.getReportedUserId(), userReportDto.getReportType());

        try {
            // 신고 처리 (모든 검증 로직은 서비스에서 처리)
            reportService.saveUserReport(reporterId, userReportDto);

            return ResponseEntity.ok(ReportResponse.success("신고가 접수되었습니다"));

        } catch (IllegalArgumentException e) {
            // 400 Bad Request: 잘못된 요청
            log.warn("Invalid report request: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ReportResponse.failure(e.getMessage()));

        } catch (IllegalStateException e) {
            // 409 Conflict: 중복 신고
            log.warn("Duplicate report attempt: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ReportResponse.failure(e.getMessage()));

        } catch (Exception e) {
            // 500 Internal Server Error
            log.error("Unexpected error while processing report", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ReportResponse.failure("신고 처리 중 오류가 발생했습니다"));
        }
    }

    // TODO: 채팅 신고 API 추가 예정
}
