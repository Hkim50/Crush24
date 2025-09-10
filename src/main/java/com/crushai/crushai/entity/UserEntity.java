package com.crushai.crushai.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name="users")
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserInfoEntity userInfo;

    @Column(unique = true)
    private String email;

    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDate premiumExpirationDate; // 프리미엄 만료일 (null이면 무료)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType; // 처음 로그인 할때 사용한 type( google, facebook, apple)

    // Apple 로그인은 sub를 유니크 키로 사용
    @Column(unique = true)
    private String appleIdSub;

    // Apple 로그인은 sub를 유니크 키로 사용
    @Column(unique = true)
    private String googleId;

    // Apple 로그인은 sub를 유니크 키로 사용
    @Column(unique = true)
    private String facebookId;

    @Column(nullable = false)
    private Boolean onboardingCompleted = false;

    // 기본 생성자
    public UserEntity(String email, String password) {
        this.email = email;
        this.password = password;
        this.role = Role.USER;
        this.loginType = LoginType.GOOGLE; // 기본값 예시
    }

    // Role 지정 생성자
    public UserEntity(String email, Role role) {
        this.email = email;
        this.role = role;
        this.loginType = LoginType.GOOGLE; // 기본값 예시
    }

    // Role + password
    public UserEntity(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.loginType = LoginType.GOOGLE; // 기본값 예시
    }

    // Role + loginType 생성자
    public UserEntity(String email, Role role, LoginType loginType) {
        this.email = email;
        this.role = role;
        this.loginType = loginType;
    }

    // for apple
    public UserEntity(String email, Role role, LoginType loginType, String appleIdSub) {
        this.email = email;
        this.role = role;
        this.loginType = loginType;
        this.appleIdSub = appleIdSub;
        // default false
        this.onboardingCompleted = false;
    }

    // for google
    public UserEntity(String email, Role role, LoginType loginType, String googleId, String nul) {
        this.email = email;
        this.role = role;
        this.loginType = loginType;
        this.googleId = googleId;
        // default false
        this.onboardingCompleted = false;
    }

    // for facebook
    public UserEntity(String email, Role role, LoginType loginType, String facebookId, String nul1, String nul2) {
        this.email = email;
        this.role = role;
        this.loginType = loginType;
        this.facebookId = facebookId;
        // default false
        this.onboardingCompleted = false;
    }

    public void upgradeToPremium(LocalDate expirationDate) {
        this.role = Role.PREMIUM;
        this.premiumExpirationDate = expirationDate;
    }

    public void downgradeToUser() {
        this.role = Role.USER;
        this.premiumExpirationDate = null;
    }
    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }
    public void setAppleIdSub(String appleIdSub) {
        this.appleIdSub = appleIdSub;
    }

    public void setOnboardingCompleted(Boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public void setUserInfo(UserInfoEntity userInfo) {
        this.userInfo = userInfo;
        if (userInfo != null) {
            userInfo.setUser(this);
        }
    }
}
