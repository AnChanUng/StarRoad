package com.kb04.starroad.Dto.mypage;

import com.kb04.starroad.Entity.Subscription;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 마이페이지 — 적금 챌린지. 가입한 상품 하나와 그 납입 기록이다. */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class ChallengeResponseDto {

    /** status 값: 만기까지 꾸준히 납입했고 리워드를 아직 받지 않았다 */
    public static final int REWARD_READY = -1;
    /** status 값: 만기가 지났지만 받을 리워드가 없다 (이미 받았거나 납입을 거른 달이 있다) */
    public static final int FINISHED = -2;

    private final int subNo;
    private final String name;
    private final String attribute;
    private final String explain;
    /** 가입 기간(개월) */
    private final int period;
    /** 매월 납입액(천원) */
    private final int price;
    /** 가입 후 n번째 달에 납입한 요일(1=월 ~ 7=일). 납입하지 않은 달은 0. 길이는 period */
    private final List<Integer> paymentDays;
    /** 가입 후 n번째 달의 납입 연월(yyyy-MM). 납입하지 않은 달은 null. 길이는 period */
    private final List<String> paymentMonths;
    /** {@link #REWARD_READY}, {@link #FINISHED}, 또는 만기까지 남은 개월 수(0 이상) */
    private final int status;

    public static ChallengeResponseDto of(Subscription subscription, List<Integer> paymentDays,
                                          List<String> paymentMonths, int status) {
        return ChallengeResponseDto.builder()
                .subNo(subscription.getNo())
                .name(subscription.getProd().getName())
                .attribute(subscription.getProd().getAttribute())
                .explain(subscription.getProd().getExplain())
                .period(subscription.getPeriod())
                .price(subscription.getPrice())
                .paymentDays(paymentDays)
                .paymentMonths(paymentMonths)
                .status(status)
                .build();
    }
}
