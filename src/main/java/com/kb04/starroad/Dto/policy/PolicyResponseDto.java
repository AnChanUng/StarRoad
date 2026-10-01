package com.kb04.starroad.Dto.policy;

import com.kb04.starroad.Entity.Policy;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class PolicyResponseDto {

    private final int no;
    private final String name;
    private final String location;
    private final String explain;
    private final String tag;
    private final String link;
    /** 로그인한 회원이 관심 정책으로 등록했으면 true. 비로그인이면 항상 false */
    private final boolean liked;

    public static PolicyResponseDto of(Policy policy, boolean liked) {
        return PolicyResponseDto.builder()
                .no(policy.getNo())
                .name(policy.getName())
                .location(policy.getLocation())
                .explain(policy.getExplain())
                .tag(policy.getTag())
                .link(policy.getLink())
                .liked(liked)
                .build();
    }
}
