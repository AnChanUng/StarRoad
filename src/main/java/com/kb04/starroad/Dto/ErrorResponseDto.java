package com.kb04.starroad.Dto;

import com.kb04.starroad.Exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 실패 응답 본문. 화면은 {@code message} 를 그대로 보여 주고, {@code code} 로 어떤 실패인지 구분한다. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponseDto {

    /** {@link ErrorCode} 의 이름. 예: BOARD_NOT_FOUND */
    private final String code;
    private final String message;

    public static ErrorResponseDto from(ErrorCode errorCode) {
        return new ErrorResponseDto(errorCode.name(), errorCode.getMessage());
    }
}
