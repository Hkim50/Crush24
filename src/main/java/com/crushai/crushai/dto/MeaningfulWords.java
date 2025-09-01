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
public class MeaningfulWords {
    private List<String> user; // 유저가 한 핵심적인 말 (예: "다음엔 불러줘" -> 상대방에게 관심 표현)
    private List<String> otherPerson; // 상대방이 한 핵심적인 말 (예: "잘 먹는 사람 좋아~" -> 긍정적인 신호)
}