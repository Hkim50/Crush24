package com.crushai.crushai.controller;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.UserInfoDto;
import com.crushai.crushai.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {

        UserInfoDto user = userProfileService.getUser(userDetails.getUsername());

        if (user == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(user);
        }
    }

    @PatchMapping("/update")
    public ResponseEntity<UserInfoDto> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserInfoDto userInfoDto) {

        UserInfoDto updatedUser = userProfileService.updateProfile(userDetails.getUsername(), userInfoDto);

        // 업데이트된 사용자 정보를 응답 본문에 담아 200 OK를 반환합니다.
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            userProfileService.deleteUser(userDetails.getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
