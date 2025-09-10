package com.crushai.crushai.service;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.UserInfoDto;
import com.crushai.crushai.entity.UserInfoEntity;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.repository.UserInfoRepository;
import com.crushai.crushai.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
@Transactional
public class UserInfoServiceImpl implements UserInfoService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;

    public UserInfoServiceImpl(UserRepository userRepository, UserInfoRepository userInfoRepository) {
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
    }

    @Override
    public ResponseEntity<?> saveUserInfo(UserInfoDto userInfoDto, List<MultipartFile> images, CustomUserDetails userDetails) {

        if (!isValidBirthdate(userInfoDto.getBirthDate())) {
            return ResponseEntity.badRequest().body(Map.of("error", "User must be at least 18 years old."));
        }

        if (images.size() < 2 || images.size() > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "user must upload 2 to 5 images."));
        }

        List<String> photoPaths = new ArrayList<>();
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            for (MultipartFile image : images) {
                String originalFilename = image.getOriginalFilename();
                String fileExtension = (originalFilename != null && originalFilename.contains("."))
                        ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

                String uniqueFilename = UUID.randomUUID() + fileExtension;
                Path filePath = uploadPath.resolve(uniqueFilename).normalize();
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                photoPaths.add("/" + uploadDir + uniqueFilename);
            }

            // 현재 사용자 엔티티 조회
            UserEntity currentUser = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + userDetails.getUsername()));

            // check if user already completed onboarding
            if (currentUser.getUserInfo() != null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User info already exists."));
            }

            // 1. DTO와 이미지 경로를 사용하여 UserInfoEntity 생성 (빌더 패턴 사용)
            UserInfoEntity userInfo = UserInfoEntity.toEntity(userInfoDto, photoPaths);

            // 2. UserEntity에 UserInfoEntity 연결 및 온보딩 상태 변경
            currentUser.setOnboardingCompleted(true);
            currentUser.setUserInfo(userInfo);

            return ResponseEntity.ok(Map.of(
                    "message", "User info and images saved successfully",
                    "photos", photoPaths
            ));
        } catch (IOException e) {
            log.error("Failed to save images", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to save images",
                    "details", e.getMessage()
            ));
        }
    }

    private boolean isValidBirthdate(Date bday) {
        if (bday == null) return false;

        // Date → LocalDate 변환
        LocalDate birthDate = bday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();

        // 나이 계산
        int age = Period.between(birthDate, today).getYears();

        return age >= 18;
    }
}
