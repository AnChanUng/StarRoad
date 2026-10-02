package com.kb04.starroad.Dto.auth;

import com.kb04.starroad.Dto.MemberDto;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/** 로그인에 성공한 회원. 비밀번호 같은 민감한 값은 담지 않는다. */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class LoginResponseDto {

    private final int no;
    private final String id;
    private final String name;

    public static LoginResponseDto from(MemberDto member) {
        return LoginResponseDto.builder()
                .no(member.getNo())
                .id(member.getId())
                .name(member.getName())
                .build();
    }
}
