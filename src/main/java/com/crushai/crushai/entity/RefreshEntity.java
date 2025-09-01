package com.crushai.crushai.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name="refresh")
@Getter
public class RefreshEntity {
    protected RefreshEntity() {}
    public RefreshEntity(String email, String refresh, String expiration) {
        this.email = email;
        this.refresh = refresh;
        this.expiration = expiration;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String refresh;
    private String expiration;
}