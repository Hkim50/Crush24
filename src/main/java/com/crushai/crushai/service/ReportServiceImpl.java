package com.crushai.crushai.service;

import com.crushai.crushai.dto.UserReportDto;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.entity.UserReportEntity;
import com.crushai.crushai.repository.UserReportRepository;
import com.crushai.crushai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;

    @Override
    @Transactional
    public void saveUserReport(Long reporterId, UserReportDto userReportDto) {
        log.info("Processing user report from user {} for user {}",
                reporterId, userReportDto.getReportedUserId());

        // 1. 신고자 검증 (컨트롤러에서 옮겨옴)
        UserEntity reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> {
                    log.error("Reporter not found: {}", reporterId);
                    return new IllegalArgumentException("신고자를 찾을 수 없습니다");
                });

        // 2. 신고 대상 사용자 검증
        UserEntity reportedUser = validateReportedUser(userReportDto.getReportedUserId());

        // 3. 자기 자신 신고 방지
        if (reporter.getId().equals(reportedUser.getId())) {
            log.warn("User {} attempted to report themselves", reporter.getId());
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다");
        }

        // 4. 중복 신고 체크
        if (userReportRepository.existsByReporterIdAndReportedUserId(
                reporter.getId(), reportedUser.getId())) {
            log.warn("User {} already reported user {}", reporter.getId(), reportedUser.getId());
            throw new IllegalStateException("이미 신고한 사용자입니다");
        }

        // 5. 신고 엔티티 생성 및 저장
        UserReportEntity reportEntity = UserReportEntity.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(userReportDto.getReportType())
                .build();

        userReportRepository.save(reportEntity);

        log.info("User report saved successfully: {} reported {} for {}",
                reporter.getId(), reportedUser.getId(), userReportDto.getReportType());
    }

    /**
     * 신고 대상 사용자 검증
     */
    private UserEntity validateReportedUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDelYn()) // 삭제되지 않은 사용자만
                .orElseThrow(() -> {
                    log.error("Reported user not found or deleted: {}", userId);
                    return new IllegalArgumentException("신고할 사용자를 찾을 수 없습니다");
                });
    }

    @Override
    public void saveChatReport(ChatReportDto chatReportDto) {
        // TODO: 채팅 신고 기능 구현
        throw new UnsupportedOperationException("채팅 신고 기능은 아직 구현되지 않았습니다");
    }
}
