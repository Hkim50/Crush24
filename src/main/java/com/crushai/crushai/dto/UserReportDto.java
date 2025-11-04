package com.crushai.crushai.dto;

import com.crushai.crushai.enums.UserReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReportDto {

    @NotNull(message = "신고할 사용자 ID는 필수입니다")
    private Long reportedUserId;
    
    @NotNull(message = "신고 사유는 필수입니다")
    private UserReportType reportType;
}
