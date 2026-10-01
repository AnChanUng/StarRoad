package com.kb04.starroad.Exception;

import com.kb04.starroad.Dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러 밖으로 나온 예외를 {@code ResponseEntity<ErrorResponseDto>} 로 바꾼다.
 * 컨트롤러는 성공 응답만 만들면 된다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StarroadException.class)
    public ResponseEntity<ErrorResponseDto> handleStarroadException(StarroadException e) {
        return ResponseEntity.status(e.getStatus()).body(ErrorResponseDto.of(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponseDto.of(e.getMessage()));
    }
}
