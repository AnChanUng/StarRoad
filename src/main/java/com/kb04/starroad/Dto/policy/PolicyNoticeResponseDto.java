package com.kb04.starroad.Dto.policy;

import com.kb04.starroad.Entity.Policy;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/** 홈 화면 알림창 — 관심 정책 중 마감이 가장 가까운 정책 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class PolicyNoticeResponseDto {

    /** 알릴 관심 정책이 있으면 true. false 면 message 만 채워진다 */
    private final boolean exists;
    /** 로그인한 회원 이름 */
    private final String userName;
    private final String policyName;
    /** 마감까지 남은 날. 0 이면 마감 당일, -5 면 5일 남음 (화면에서 "D-5" 로 쓴다) */
    private final Long dday;
    private final String link;
    /** 알릴 정책이 없을 때 보여 줄 안내 문구 */
    private final String message;

    /** 알릴 관심 정책이 있을 때 */
    public static PolicyNoticeResponseDto of(String userName, Policy policy, long dday) {
        return PolicyNoticeResponseDto.builder()
                .exists(true)
                .userName(userName)
                .policyName(policy.getName())
                .dday(dday)
                .link(policy.getLink())
                .build();
    }

    /** 관심 정책이 없거나 모두 마감됐을 때 */
    public static PolicyNoticeResponseDto empty(String message) {
        return PolicyNoticeResponseDto.builder()
                .exists(false)
                .message(message)
                .build();
    }
}
