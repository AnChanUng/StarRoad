package com.kb04.starroad.Entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "policy_heart")
public class PolicyHeart {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policy_heart_seq")
    @SequenceGenerator(name = "policy_heart_seq", sequenceName = "POLICY_HEART_SEQ", allocationSize = 1)
    @Column(name = "no")
    private int no;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_no")
    private Member member;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_no")
    private Policy policy;

    /** 회원이 관심 정책으로 등록한 정책 */
    public static PolicyHeart of(Member member, Policy policy) {
        return PolicyHeart.builder()
                .member(member)
                .policy(policy)
                .build();
    }
}
