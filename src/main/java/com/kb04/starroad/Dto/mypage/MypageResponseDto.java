package com.kb04.starroad.Dto.mypage;

import com.kb04.starroad.Entity.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/** 마이페이지 — 나의 자산. 금액 단위는 천원이다. */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class MypageResponseDto {
        private final String name;    // 이름
        private final int point;      // 포인트리
        private final int investment; // 투자금
        private final int savings;    // 적금
        private final int deposit;    // 예금

        /**
         * @param savings 적금 납입 합계. 납입 내역이 없으면 null
         * @param deposit 예금 납입 합계. 납입 내역이 없으면 null
         */
        public static MypageResponseDto of(Member member, Long savings, Long deposit) {
                return MypageResponseDto.builder()
                        .name(member.getName())
                        .point(member.getPoint())
                        .investment(member.getInvestment())
                        .savings(savings == null ? 0 : savings.intValue())
                        .deposit(deposit == null ? 0 : deposit.intValue())
                        .build();
        }
}
