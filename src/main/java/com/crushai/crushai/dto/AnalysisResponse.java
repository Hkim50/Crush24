package com.crushai.crushai.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResponse {
    private int crushLevel; // 1-100 (상대방의 유저에 대한 관심도)
    private MeaningfulWords meaningfulWords; // 핵심 포인트 (단어만)
    private List<String> redFlags; // 위험 신호 (간략한 설명, 3개 이내)
    private List<String> greenFlags; // 긍정 신호 (간략한 설명, 3개 이내)
    private String nextMove; // 다음 대화 전략 추천 (하나의 무브만)

    private String vibeSummary; // 대화 분위기 요약 (1-2줄 간략)
    private SimplicityVsEffortMetrics simplicityVsEffort; // 상대의 정성 지수 (숫자만)
}