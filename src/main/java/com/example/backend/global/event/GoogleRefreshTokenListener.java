package com.example.backend.global.event;

public record GoogleRefreshTokenListener(String username, String googleRefreshToken) {}