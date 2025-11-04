package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    
    private boolean success;
    private String message;
    
    public static ReportResponse success(String message) {
        return ReportResponse.builder()
                .success(true)
                .message(message)
                .build();
    }
    
    public static ReportResponse failure(String message) {
        return ReportResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
