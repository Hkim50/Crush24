package com.crushai.crushai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @GetMapping("/welcome")
    public ResponseEntity<?> adminP() {
        return ResponseEntity.ok("Welcome to CrushAI!");
    }
}
