package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwipeCardDto {
    private Long userId;
    private String nickname;
    private Integer age;
    private String location;
    private List<String> photos;
    private Boolean likedByThem;  // 이 유저가 나를 좋아요 했는지
}
