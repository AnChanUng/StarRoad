package com.kb04.starroad.Entity;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SequenceGenerator(name = "subscription_seq", sequenceName = "subscription_seq")
@Table(name = "subscription")
public class Subscription {

    /** 리워드를 아직 받지 않음 */
    public static final char REWARD_NOT_RECEIVED = '0';
    /** 리워드를 받음 */
    public static final char REWARD_RECEIVED = '1';

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscription_seq")
    private int no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_no", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prod_no", nullable = false)
    private Product prod;

    @Column(nullable = false)
    private int period;

    @Column(nullable = false)
    private int price;

    @Builder.Default
    @Column(nullable = false)
    private char received = REWARD_NOT_RECEIVED;    // 0: 받지 않음, 1: 받음

    /**
     * 회원이 상품에 가입한다. 리워드는 아직 받지 않은 상태로 시작한다.
     *
     * @param period 가입 기간(개월)
     * @param price  매월 납입액(천원)
     */
    public static Subscription subscribe(Member member, Product prod, int period, int price) {
        return Subscription.builder()
                .member(member)
                .prod(prod)
                .period(period)
                .price(price)
                .received(REWARD_NOT_RECEIVED)
                .build();
    }

    /** 이 가입 건의 주인이 맞는지 */
    public boolean isOwnedBy(int memberNo) {
        return member.getNo() == memberNo;
    }

    /** 만기 리워드를 받았다고 기록한다 */
    public void receiveReward() {
        this.received = REWARD_RECEIVED;
    }
}
