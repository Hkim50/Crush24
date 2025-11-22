package com.crushai.crushai.controller;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.UserInfoDto;
import com.crushai.crushai.service.UserInfoService;
import com.crushai.crushai.service.UserInfoServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OnboardingController {

    private final UserInfoService userInfoService;

    public OnboardingController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    /**
     * 온보딩 완료 API
     * 
     * 프로필 정보(이름, 생년월일, 성별, 사진 등)만 저장합니다.
     * 
     * ⚠️ 중요: 위치 정보는 별도로 저장해야 합니다!
     * 온보딩 완료 후 클라이언트에서 POST /api/location/save 를 즉시 호출하세요.
     * (Fire-and-forget 방식으로 응답을 기다리지 않고 호출)
     * 
     * @param userInfoDto 사용자 프로필 정보 (위치 정보 제외)
     * @param images 프로필 사진 (2-5장)
     * @param userDetails 인증된 사용자 정보
     * @return 온보딩 완료 응답
     */
    @PostMapping(value = "/onboarding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createUserInfo(@RequestPart("userInfo") @Valid UserInfoDto userInfoDto,
                                            @RequestPart("images") List<MultipartFile> images,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (images.size() > 5 || images.size() < 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You can upload a maximum of 5 photos, with a minimum of 2.");
        }

        return userInfoService.saveUserInfo(userInfoDto, images, userDetails);
    }

}
