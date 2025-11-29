package com.nevgiu.hrai.chat.dto;

public record ChatRequestDto(
        Long sessionId,
        String userId,
        String message
) {}

