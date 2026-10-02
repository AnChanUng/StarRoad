package com.kb04.starroad.Exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서비스가 클라이언트에게 알리는 실패를 한곳에 모은 목록.
 *
 * <p>실패 하나가 상태 코드와 안내 문구를 함께 가진다. 새 실패가 생기면 여기에 한 줄을 더하고
 * {@code throw new StarroadException(ErrorCode.XXX)} 로 던진다. 응답으로 바꾸는 일은
 * {@link GlobalExceptionHandler} 가 한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 — 요청 값이 잘못됐다
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    LOGIN_ID_REQUIRED(HttpStatus.BAD_REQUEST, "아이디를 입력해주세요"),
    LOGIN_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "비밀번호를 입력해주세요"),
    INVALID_MEMBER_INFO(HttpStatus.BAD_REQUEST, "회원정보를 다시 확인해주세요"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호를 다시 확인해주세요."),
    INVALID_BOARD_TYPE(HttpStatus.BAD_REQUEST, "잘못된 type 값입니다."),
    COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "댓글 내용을 입력해주세요."),

    // 401 — 로그인이 필요하다
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다"),
    // 아이디가 없는지 비밀번호가 틀렸는지는 알려 주지 않는다
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디와 비밀번호가 일치하지 않습니다"),

    // 403 — 로그인은 했지만 권한이 없다
    BOARD_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "다른 사용자의 게시물을 수정할 수 없습니다."),
    BOARD_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "삭제 할 권한이 없습니다."),
    COMMENT_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "다른 사용자의 댓글을 수정할 수 없습니다."),
    COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "삭제 할 권한이 없습니다."),
    SUBSCRIPTION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 가입한 상품이 아닙니다."),

    // 404 — 대상이 없다
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 댓글이 존재하지 않습니다."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "정책을 찾을 수 없습니다."),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "가입한 상품을 찾을 수 없습니다."),

    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),

    // 405 — 그 주소가 받지 않는 요청 방식이다
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),

    // 409 — 이미 처리됐거나 중복이다
    DUPLICATE_MEMBER_ID(HttpStatus.CONFLICT, "이미 사용중인 아이디입니다."),
    DUPLICATE_MEMBER_EMAIL(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."),
    BOARD_ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요한 게시글입니다."),
    REWARD_NOT_AVAILABLE(HttpStatus.CONFLICT, "리워드를 받을 수 없는 상품입니다."),

    // 500 — 서버에서 처리하지 못했다
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;
}
