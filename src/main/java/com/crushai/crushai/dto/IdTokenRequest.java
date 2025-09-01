package com.crushai.crushai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdTokenRequest {
    private String idToken;
    private String authCode;
}
