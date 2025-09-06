package com.crushai.crushai.service;

import com.crushai.crushai.dto.UserInfoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

public interface UserInfoService {

    // has to be 18 or older
    Boolean isValidAge(Date birthDate);

    // check if image is greater than 2 and less than 5
    Boolean isValidImages(List<MultipartFile> images);

    ResponseEntity<?> saveUserInfo(UserInfoDto userInfoDto, List<MultipartFile> images) throws IllegalArgumentException;
}
