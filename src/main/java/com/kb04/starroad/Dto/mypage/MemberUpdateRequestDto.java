package com.kb04.starroad.Dto.mypage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 마이페이지 — 내 정보 수정 요청. email·job·purpose·source 는 보내지 않으면 기존 값을 유지한다. */
@Getter
@Setter
@NoArgsConstructor
public class MemberUpdateRequestDto {

    /** 010-1234-5678 형식 */
    private String phone;
    private String email;
    /** "기본주소,상세주소" 형식 */
    private String address;
    private String job;
    /** 월수입(천원) */
    private int salary;
    private String purpose;
    private String source;
    /** 저금 목표치(%) */
    private int goal;
}
