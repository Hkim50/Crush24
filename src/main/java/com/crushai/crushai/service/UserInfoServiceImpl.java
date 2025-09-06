package com.crushai.crushai.service;

import com.crushai.crushai.dto.UserInfoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService{

//    private final UserInfoRespository userInfoRespository;
//
//    public UserInfoServiceImpl(UserInfoRespository userInfoRespository) {
//        this.userInfoRespository = userInfoRespository;
//    }

    @Override
    public Boolean isValidAge(Date birthDate) {
        return null;
    }

    @Override
    public Boolean isValidImages(List<MultipartFile> images) {
        return null;
    }

    @Override
    public ResponseEntity<?> saveUserInfo(UserInfoDto userInfoDto) throws IllegalArgumentException {
        return null;
    }

}
