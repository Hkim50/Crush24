package com.crushai.crushai.controller;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.UserInfoDto;
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
public class UserInfoController {

    private final UserInfoServiceImpl userInfoService;

    public UserInfoController(UserInfoServiceImpl userInfoService) {
        this.userInfoService = userInfoService;
    }

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
