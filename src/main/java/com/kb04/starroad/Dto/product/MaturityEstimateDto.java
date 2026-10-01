package com.kb04.starroad.Dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 회원 한 명이 상품 하나에 가입했을 때의 만기 예상 금액.
 *
 * <p>금액 단위는 모두 원이다. {@code total = principal + interest - tax}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaturityEstimateDto {

    /** 계산에 쓴 가입 기간(개월) */
    private int months;

    /** 회원에게 실제로 적용되는 연 이율(%). 기본 금리 + 충족한 우대금리 */
    private BigDecimal appliedRate;

    /** 매월 납입액 */
    private long monthlyAmount;

    /** 납입 원금 합계 */
    private long principal;

    /** 세전 이자 */
    private long interest;

    /** 이자에 붙는 세금 */
    private long tax;

    /** 세후 만기 수령액 */
    private long total;
}
