package com.kb04.starroad.Dto.member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 아이디·이메일 중복 확인 결과 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DuplicateCheckResponseDto {

    /** 이미 쓰고 있는 값이면 true */
    private final boolean duplicated;

    public static DuplicateCheckResponseDto of(boolean duplicated) {
        return new DuplicateCheckResponseDto(duplicated);
    }
}
