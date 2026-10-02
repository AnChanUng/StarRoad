package com.kb04.starroad.Dto.board;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 좋아요를 누른 뒤의 좋아요 수 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LikeResponseDto {

    private final int likes;

    public static LikeResponseDto of(int likes) {
        return new LikeResponseDto(likes);
    }
}
