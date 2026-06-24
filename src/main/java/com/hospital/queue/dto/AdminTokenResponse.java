package com.hospital.queue.dto;

public record AdminTokenResponse(String accessToken, String tokenType, long expiresIn) {}
