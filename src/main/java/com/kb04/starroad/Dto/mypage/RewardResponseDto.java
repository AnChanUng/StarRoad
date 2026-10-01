package com.kb04.starroad.Dto.mypage;

import com.kb04.starroad.Entity.Subscription;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/** 적금 챌린지 완주 리워드 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class RewardResponseDto {

    private final int subNo;
    /** 상품 이름 */
    private final String name;
    /** 가입 기간(개월) */
    private final int period;
    /** 받을(받은) 포인트리 */
    private final int reward;

    public static RewardResponseDto of(Subscription subscription, int reward) {
        return RewardResponseDto.builder()
                .subNo(subscription.getNo())
                .name(subscription.getProd().getName())
                .period(subscription.getPeriod())
                .reward(reward)
                .build();
    }
}
