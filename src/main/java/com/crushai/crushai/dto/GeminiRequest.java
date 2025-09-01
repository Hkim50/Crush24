package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {
    private List<Content> contents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
        private InlineData inlineData; // 이미지 데이터를 위한 필드
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InlineData {
        private String mimeType;
        private String data; // Base64 인코딩된 이미지 데이터
    }
}