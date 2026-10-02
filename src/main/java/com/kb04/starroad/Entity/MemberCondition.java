package com.kb04.starroad.Entity;

import lombok.*;

import jakarta.persistence.*;

@Getter
@Entity
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "member_condition")
public class MemberCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_condition_seq")
    @SequenceGenerator(name = "member_condition_seq", sequenceName = "MEMBER_CONDITION_SEQ", allocationSize = 1)
    @Column(name = "no")
    private int no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_no", nullable = false)
    private Condition condition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_no", nullable = false)
    private Member member;

    /** 회원이 충족한 우대 조건 */
    public static MemberCondition of(Member member, Condition condition) {
        return MemberCondition.builder()
                .member(member)
                .condition(condition)
                .build();
    }
}
