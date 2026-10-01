package com.kb04.starroad.Entity;

import lombok.*;


import jakarta.persistence.*;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@Table(name = "member")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    private static final String NO_JOB = "직업없음";
    private static final String NO_PURPOSE = "목적없음";
    private static final String NO_SOURCE = "원천없음";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_seq", allocationSize = 1)
    @Column(name = "no", nullable = false)
    private int no;

    @Column(name = "name", nullable = false, length = 15)
    private String name;

    @Column(name = "id", nullable = false, length = 15, unique = true)
    private String id;

    @Column(name = "password", nullable = false, length = 3000)
    private String password;

    @Column(name = "birthday", nullable = false)
    private String birthday;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "email", nullable = false, length = 50, unique = true)
    private String email;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Builder.Default
    @Column(name = "job", nullable = false, length = 30)
    private String job = "N";

    @Builder.Default
    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose = "N";

    @Builder.Default
    @Column(name = "source", nullable = false, length = 50)
    private String source = "N";

    @Builder.Default
    @Column(name = "goal", nullable = false)
    private int goal = 0;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private Character status = 'Y';

    @Builder.Default
    @Column(name = "salary", nullable = false)
    private int salary = 0;

    @Builder.Default
    @Column(name = "agreement", nullable = false)
    private Character agreement = 'Y';

    @Builder.Default
    @Column(name = "point", nullable = false)
    private int point = 0;

    @Builder.Default
    @Column(name = "investment", nullable = false)
    private int investment = 0;

    /**
     * 회원가입. 상태는 활성('Y'), 약관 동의('Y'), 포인트와 투자금은 0 으로 시작한다.
     *
     * @param encodedPassword 암호화한 비밀번호. 평문을 넘기면 안 된다
     * @param job             직업. 고르지 않았으면 null
     * @param purpose         거래 목적. 고르지 않았으면 null
     * @param source          거래 자금의 원천. 고르지 않았으면 null
     * @param salary          월수입(천원)
     * @param goal            저금 목표치(%)
     */
    public static Member join(String name, String id, String encodedPassword, String birthday,
                              String phone, String email, String address,
                              String job, String purpose, String source, int salary, int goal) {
        return Member.builder()
                .name(name)
                .id(id)
                .password(encodedPassword)
                .birthday(birthday)
                .phone(phone)
                .email(email)
                .address(address)
                .job(job != null ? job : NO_JOB)
                .purpose(purpose != null ? purpose : NO_PURPOSE)
                .source(source != null ? source : NO_SOURCE)
                .salary(salary)
                .goal(goal)
                .build();
    }

    public void savePoint(int rewardPoint) {
        this.point += rewardPoint;
    }
}
