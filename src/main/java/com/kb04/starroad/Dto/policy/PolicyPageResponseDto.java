package com.kb04.starroad.Dto.policy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 청년정책 목록 한 페이지 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyPageResponseDto {

    private final List<PolicyResponseDto> policyList;
    /** 마지막 페이지 번호 */
    private final int pageEndIndex;
    private final int currentPage;

    public static PolicyPageResponseDto of(List<PolicyResponseDto> policyList, int pageEndIndex, int currentPage) {
        return new PolicyPageResponseDto(policyList, pageEndIndex, currentPage);
    }
}
