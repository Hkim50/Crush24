package com.crushai.crushai.repository;

import com.crushai.crushai.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByAppleIdSub(String sub);

    Optional<UserEntity> findByFacebookId(String facebookId);

    Optional<UserEntity> findByGoogleId(String googleId);

    List<UserEntity> findAllByDeletedAtTrueAndDeletedAtBefore(Instant now);

}
