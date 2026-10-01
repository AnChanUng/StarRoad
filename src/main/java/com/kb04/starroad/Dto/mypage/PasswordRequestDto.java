package com.kb04.starroad.Dto.mypage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 비밀번호 확인·수정 요청 */
@Getter
@Setter
@NoArgsConstructor
public class PasswordRequestDto {

    private String password;
}
