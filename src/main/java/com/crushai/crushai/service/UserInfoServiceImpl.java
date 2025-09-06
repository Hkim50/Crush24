package com.crushai.crushai.service;

import com.crushai.crushai.dto.UserInfoDto;
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
public class UserInfoServiceImpl implements UserInfoService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public ResponseEntity<?> saveUserInfo(UserInfoDto userInfoDto, List<MultipartFile> images) {

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

                if (!fileExtension.toLowerCase().matches("\\.(jpg|jpeg|png|gif|heic)$")) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed."));
                }

                String uniqueFilename = UUID.randomUUID() + fileExtension;
                Path filePath = uploadPath.resolve(uniqueFilename).normalize();
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                photoPaths.add("/" + uploadDir + uniqueFilename);
            }

            userInfoDto.setPhotos(photoPaths);

            // TODO: Save to DB
            // userInfoRepository.save(userInfoDto);

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
