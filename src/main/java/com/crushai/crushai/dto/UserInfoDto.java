package com.crushai.crushai.dto;

import com.crushai.crushai.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {

    private String name;

    // 생년월일: 18세 이상 검증을 위해 NotNull 및 과거 날짜 검증
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private Date birthDate;

    // 성별: 필수 선택
    private Gender gender; //  (예: Gender.MALE, Gender.FEMALE)

    // 매칭 대상 성별: 최소 1개
    @Size(min = 1, message = "매칭 대상 성별은 최소 하나 이상 선택해야 합니다.")
    private List<Gender> showMeGender;

    // 사진: 2장에서 5장
    private List<String> photos; // 이미지 URL 또는 파일 이름 리스트

    public UserInfoDto(Gender gender, String name, List<Gender> showMeGender, List<String> photos) {
        this.gender = gender;
        this.name = name;
        this.showMeGender = showMeGender;
        this.photos = photos;
    }

    public UserInfoDto(String name, Date birthDate, Gender gender, List<Gender> showMeGender, List<String> photos) {
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.showMeGender = showMeGender;
        this.photos = photos;
    }
}
