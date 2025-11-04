package com.crushai.crushai.service;

import com.crushai.crushai.dto.UserReportDto;

public interface ReportService {

    // for report for user
    void saveUserReport(Long reporterId, UserReportDto userReportDto);

    void saveChatReport(ChatReportDto chatReportDto);
}
