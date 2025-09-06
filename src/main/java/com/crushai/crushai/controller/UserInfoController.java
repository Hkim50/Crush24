package com.crushai.crushai.controller;

import com.crushai.crushai.dto.UserInfoDto;
import com.crushai.crushai.service.UserInfoServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/info")
public class UserInfoController {

    private final UserInfoServiceImpl userInfoService;

    public UserInfoController(UserInfoServiceImpl userInfoService) {
        this.userInfoService = userInfoService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createUserInfo(@RequestPart("userInfo") @Valid UserInfoDto userInfoDto,
                                            @RequestPart("images") List<MultipartFile> images) {

        if (images.size() > 5 || images.size() < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return userInfoService.saveUserInfo(userInfoDto, images);
    }
}
