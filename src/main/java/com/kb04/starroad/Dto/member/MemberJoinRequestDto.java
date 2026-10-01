package com.kb04.starroad.Dto.member;

import com.kb04.starroad.Entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 회원가입 요청 */
@Getter
@Setter
@NoArgsConstructor
public class MemberJoinRequestDto {

    private String name;
    private String id;
    private String password;
    private String birthday;
    /** 010-1234-5678 형식 */
    private String phone;
    private String email;
    /** "기본주소,상세주소" 형식 */
    private String address;
    private String job;
    private String purpose;
    private String source;
    /** 월수입(천원) */
    private int salary;
    /** 저금 목표치(%) */
    private int goal;

    /**
     * @param encodedPassword 암호화한 비밀번호. 요청에 담긴 평문은 저장하지 않는다
     */
    public Member toEntity(String encodedPassword) {
        return Member.join(name, id, encodedPassword, birthday, phone, email, address,
                job, purpose, source, salary, goal);
    }
}
