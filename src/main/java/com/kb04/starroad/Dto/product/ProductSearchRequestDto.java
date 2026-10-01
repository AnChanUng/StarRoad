package com.kb04.starroad.Dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 예적금 상품 검색 조건. type·period·query 를 모두 비우면 전체를 조회한다. */
@Getter
@Setter
@NoArgsConstructor
public class ProductSearchRequestDto {

    /** 페이지 번호 (1부터) */
    private int page = 1;
    /** 상품 유형 (S: 적금, D: 예금) */
    private String type;
    /** 최대 가능 가입 기간(개월) */
    private String period;
    /** 이자 과세 (base: 일반과세, none: 비과세) */
    private String rate;
    /** 상품명 */
    private String query;
}
