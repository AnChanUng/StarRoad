package com.kb04.starroad.Dto.policy;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 청년정책 검색 조건. 아무 조건도 주지 않으면 전체를 조회한다. */
@Getter
@Setter
@NoArgsConstructor
public class PolicyRequestDto {

    /** 페이지 번호 (1부터) */
    private int pageIndex = 1;
    /** 정책명 키워드 */
    private String keyword;
    /** 지역 */
    private String location;
    /** 금융지원 TAG */
    private String tag1;
    /** 교육 TAG */
    private String tag2;
    /** 생활지원 TAG */
    private String tag3;
    /** 금융자산 형성 TAG */
    private String tag4;
}
