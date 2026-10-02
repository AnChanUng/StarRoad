package com.kb04.starroad.Dto.policy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 관심 정책 등록·해제 결과 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyLikeResponseDto {

    /** 요청을 처리한 뒤 관심 정책으로 등록된 상태면 true */
    private final boolean liked;

    public static PolicyLikeResponseDto of(boolean liked) {
        return new PolicyLikeResponseDto(liked);
    }
}
