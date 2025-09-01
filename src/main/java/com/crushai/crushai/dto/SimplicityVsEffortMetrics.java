package com.crushai.crushai.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimplicityVsEffortMetrics {
    private int avgMessageLengthWords; // 메시지 평균 길이 (단어 수)
    private double questionRatio; // 질문 포함 비율 (0.0 ~ 1.0)
}