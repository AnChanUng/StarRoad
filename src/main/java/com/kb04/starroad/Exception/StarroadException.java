package com.kb04.starroad.Exception;

import lombok.Getter;

/**
 * API 처리 중 클라이언트에게 알려야 하는 실패.
 *
 * <p>무엇이 실패했는지는 {@link ErrorCode} 가 말해 준다. {@link GlobalExceptionHandler} 가 받아서
 * 상태 코드와 메시지를 담은 응답으로 바꾼다.
 */
@Getter
public class StarroadException extends RuntimeException {

    private final ErrorCode errorCode;

    public StarroadException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
