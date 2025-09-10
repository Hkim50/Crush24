package com.crushai.crushai.entity;

import com.crushai.crushai.dto.UserInfoDto;
import com.crushai.crushai.enums.Gender;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Entity
@Table(name = "user_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 기본 생성자. 외부에서는 사용하지 못하도록 PROTECTED로 설정
public class UserInfoEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private UserEntity user;

    private String nickname;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String location;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_info_show_me_genders", joinColumns = @JoinColumn(name = "user_info_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private List<Gender> showMeGender;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_photos", joinColumns = @JoinColumn(name = "user_info_id"))
    @Column(name = "photo_url")
    @OrderColumn(name = "photo_order")
    private List<String> photoUrls;

    @Builder
    public UserInfoEntity(String nickname, LocalDate birthDate, Gender gender, String location, List<Gender> showMeGender, List<String> photoUrls) {
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.gender = gender;
        this.location = location;
        this.showMeGender = showMeGender;
        this.photoUrls = photoUrls;
    }

    // DTO를 Entity로 변환하는 정적 팩토리 메소드
    public static UserInfoEntity toEntity(UserInfoDto dto, List<String> photoPaths) {
        return UserInfoEntity.builder()
                .nickname(dto.getName())
                .birthDate(dto.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .gender(dto.getGender())
                .location(dto.getLocation())
                .showMeGender(dto.getShowMeGender())
                .photoUrls(photoPaths)
                .build();
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}