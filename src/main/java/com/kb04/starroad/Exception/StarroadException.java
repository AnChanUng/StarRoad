package com.kb04.starroad.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 처리 중 클라이언트에게 알려야 하는 실패.
 *
 * <p>상태 코드별 정적 팩토리로 만든다. {@link GlobalExceptionHandler} 가 받아서
 * 상태 코드와 메시지를 담은 응답으로 바꾼다.
 */
@Getter
public class StarroadException extends RuntimeException {

    private final HttpStatus status;

    private StarroadException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** 400 — 요청 값이 잘못됐다 */
    public static StarroadException badRequest(String message) {
        return new StarroadException(HttpStatus.BAD_REQUEST, message);
    }

    /** 401 — 로그인이 필요하다 */
    public static StarroadException unauthorized(String message) {
        return new StarroadException(HttpStatus.UNAUTHORIZED, message);
    }

    /** 403 — 로그인은 했지만 권한이 없다 */
    public static StarroadException forbidden(String message) {
        return new StarroadException(HttpStatus.FORBIDDEN, message);
    }

    /** 404 — 대상이 없다 */
    public static StarroadException notFound(String message) {
        return new StarroadException(HttpStatus.NOT_FOUND, message);
    }

    /** 409 — 이미 처리됐거나 중복이다 */
    public static StarroadException conflict(String message) {
        return new StarroadException(HttpStatus.CONFLICT, message);
    }

    /** 500 — 서버에서 처리하지 못했다 */
    public static StarroadException internal(String message) {
        return new StarroadException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
