package com.crushai.crushai.service;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.UserInfoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

public interface UserInfoService {

    ResponseEntity<?> saveUserInfo(UserInfoDto userInfoDto, List<MultipartFile> images, CustomUserDetails userDetails) throws IllegalArgumentException;
}
