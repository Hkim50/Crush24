package com.crushai.crushai.entity;

public enum TokenStatus {
    ACTIVE,    // 활성 토큰
    EXPIRED,   // 만료됨
    INVALID    // 무효 (앱 삭제, 토큰 거부 등)
}
