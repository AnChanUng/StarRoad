package com.kb04.starroad.Dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 실패 응답 본문. 화면은 {@code message} 를 그대로 보여 준다. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponseDto {

    private final String message;

    public static ErrorResponseDto of(String message) {
        return new ErrorResponseDto(message);
    }
}
