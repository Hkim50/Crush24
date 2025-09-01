package com.crushai.crushai.service;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UserEntity> userEntity = userRepository.findByEmail(email);

        if (userEntity.isPresent()) {
            return new CustomUserDetails(userEntity.get());
        }
        return null;

    }

}
