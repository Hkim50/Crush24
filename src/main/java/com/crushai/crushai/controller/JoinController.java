package com.crushai.crushai.controller;

import com.crushai.crushai.dto.JoinDto;
import com.crushai.crushai.service.JoinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JoinController {

    private final JoinService joinService;

    public JoinController(JoinService joinService) {
        this.joinService = joinService;
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinProcess(@RequestBody JoinDto dto) {
        try {
            joinService.join(dto);
            return ResponseEntity.ok().body("{\"message\": \"회원가입 성공\"}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

}
