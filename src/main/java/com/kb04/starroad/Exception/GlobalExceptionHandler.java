package com.kb04.starroad.Exception;

import com.kb04.starroad.Dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 컨트롤러 밖으로 나온 예외를 모두 여기서 받아 {@code ResponseEntity<ErrorResponseDto>} 로 바꾼다.
 * 컨트롤러와 서비스는 성공 응답만 만들고, 실패는 {@link StarroadException} 을 던지기만 하면 된다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스가 알고 던진 실패 — {@link ErrorCode} 에 적힌 상태 코드와 문구로 응답한다 */
    @ExceptionHandler(StarroadException.class)
    public ResponseEntity<ErrorResponseDto> handleStarroadException(StarroadException e) {
        return toResponse(e.getErrorCode());
    }

    /** 요청 값을 읽지 못했다 — JSON 형식이 틀렸거나, 숫자 자리에 글자가 왔거나, 값의 범위가 맞지 않는다 */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ErrorResponseDto> handleInvalidRequest(Exception e) {
        log.warn("[잘못된 요청] {}", e.getMessage());
        return toResponse(ErrorCode.INVALID_REQUEST);
    }

    /**
     * 그 밖의 모든 예외.
     *
     * <p>스프링이 던지는 표준 예외(없는 주소, 받지 않는 요청 방식, 필수 파라미터 누락 등)는 그 상태 코드에 맞는
     * {@link ErrorCode} 로 바꾼다. 나머지는 예상하지 못한 예외다 — 원인은 로그에만 남기고,
     * 응답에는 내부 사정(스택 트레이스, SQL)을 싣지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            return toResponse(toErrorCode(errorResponse.getStatusCode()));
        }
        log.error("[처리하지 못한 예외]", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private static ErrorCode toErrorCode(HttpStatusCode status) {
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.NOT_FOUND;
        }
        if (status.value() == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        return status.is4xxClientError() ? ErrorCode.INVALID_REQUEST : ErrorCode.INTERNAL_SERVER_ERROR;
    }

    private static ResponseEntity<ErrorResponseDto> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponseDto.from(errorCode));
    }
}
