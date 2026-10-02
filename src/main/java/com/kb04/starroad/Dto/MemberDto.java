package com.kb04.starroad.Dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kb04.starroad.Entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 세션에 담아 두는 로그인 회원 정보.
 * 암호화된 비밀번호가 들어 있으므로 API 응답으로 내보내지 않는다 — 응답은 {@code MemberResponseDto} 를 쓴다.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDto {

    private int no;
    private String name;
    private String id;
    @JsonIgnore
    private String password;
    private String birthday;
    private String phone;
    private String email;
    private String address;
    private String job;
    private String purpose;
    private String source;
    private int goal;
    private Character status;
    private int salary;
    private Character agreement;
    private int point;
    private int investment;

    public static MemberDto from(Member member) {
        return MemberDto.builder()
                .no(member.getNo())
                .name(member.getName())
                .id(member.getId())
                .password(member.getPassword())
                .birthday(member.getBirthday())
                .phone(member.getPhone())
                .email(member.getEmail())
                .address(member.getAddress())
                .job(member.getJob())
                .purpose(member.getPurpose())
                .source(member.getSource())
                .goal(member.getGoal())
                .status(member.getStatus())
                .salary(member.getSalary())
                .agreement(member.getAgreement())
                .point(member.getPoint())
                .investment(member.getInvestment())
                .build();
    }

}
