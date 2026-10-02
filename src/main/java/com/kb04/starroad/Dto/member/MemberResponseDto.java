package com.kb04.starroad.Dto.member;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Entity.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/** 화면에 내려 주는 회원 정보. 비밀번호는 담지 않는다. */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class MemberResponseDto {

    private final int no;
    private final String name;
    private final String id;
    private final String birthday;
    private final String phone;
    private final String email;
    private final String address;
    private final String job;
    private final String purpose;
    private final String source;
    private final int salary;
    private final int goal;
    private final int point;

    public static MemberResponseDto from(Member member) {
        return from(MemberDto.from(member));
    }

    public static MemberResponseDto from(MemberDto member) {
        return MemberResponseDto.builder()
                .no(member.getNo())
                .name(member.getName())
                .id(member.getId())
                .birthday(member.getBirthday())
                .phone(member.getPhone())
                .email(member.getEmail())
                .address(member.getAddress())
                .job(member.getJob())
                .purpose(member.getPurpose())
                .source(member.getSource())
                .salary(member.getSalary())
                .goal(member.getGoal())
                .point(member.getPoint())
                .build();
    }
}
