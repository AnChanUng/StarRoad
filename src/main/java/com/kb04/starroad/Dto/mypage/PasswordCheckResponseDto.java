package com.kb04.starroad.Dto.mypage;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 비밀번호 확인 결과 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordCheckResponseDto {

    /** 입력한 비밀번호가 맞으면 true */
    private final boolean matched;

    public static PasswordCheckResponseDto of(boolean matched) {
        return new PasswordCheckResponseDto(matched);
    }
}
