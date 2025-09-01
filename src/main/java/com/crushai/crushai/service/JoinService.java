package com.crushai.crushai.service;

import com.crushai.crushai.dto.JoinDto;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JoinService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public JoinService(UserRepository repository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.repository = repository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public void join(JoinDto dto) {

        if (dto.getEmail() == null || dto.getPassword() == null) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 비어 있습니다.");
        }

        if (repository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }

        UserEntity entity = new UserEntity(dto.getEmail(), bCryptPasswordEncoder.encode(dto.getPassword()));
        repository.save(entity);
    }
}
