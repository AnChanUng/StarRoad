package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.product.MaturityEstimateDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 만기 수령액 계산.
 *
 * <p>같은 상품이라도 충족한 우대 조건이 사람마다 달라서 실제로 받는 이율이 다르다.
 * 그래서 계산을 세 단계로 나눈다.
 * <ol>
 *   <li>{@link #personalRate} — 이 회원에게 적용되는 이율을 정한다</li>
 *   <li>{@link #term} — 계산할 가입 기간을 정한다</li>
 *   <li>{@link #estimate} — 매월 납입액·기간·이율·세율로 세후 수령액을 계산한다</li>
 * </ol>
 *
 * <p>DB 나 세션에 의존하지 않는다. 입력이 같으면 결과가 같아서 단위 테스트로 검증한다.
 */
@Component
public class MaturityCalculator {

    /** 일반과세: 이자소득세 14% + 지방소득세 1.4% */
    public static final double GENERAL_TAX_RATE = 0.154;
    public static final double TAX_FREE = 0.0;

    private static final BigDecimal MONTHS_PER_YEAR_PERCENT = BigDecimal.valueOf(1200);

    /**
     * 회원에게 적용되는 연 이율(%).
     *
     * <p>기본 금리에 회원이 충족한 우대금리만 더한다. 상품의 최고 금리({@code maxRate})는
     * 우대 조건을 전부 채웠을 때의 값이라, 그대로 쓰면 누구에게나 최고 금리를 준 것처럼 계산된다.
     *
     * @param maxRate             상품 최고 금리 (기본 금리 + 최대 우대금리)
     * @param maxConditionRate    상품의 최대 우대금리. 우대 조건이 없는 상품은 null
     * @param periodBaseRate      가입 기간에 해당하는 기본 금리. 기간을 고르지 않았으면 null
     * @param memberConditionRate 회원이 충족한 우대 조건의 금리 합
     */
    public BigDecimal personalRate(double maxRate, Double maxConditionRate, Double periodBaseRate,
                                   double memberConditionRate) {
        BigDecimal max = BigDecimal.valueOf(maxRate);
        BigDecimal maxPreferential = maxConditionRate == null ? null : BigDecimal.valueOf(maxConditionRate);

        BigDecimal base = periodBaseRate != null
                ? BigDecimal.valueOf(periodBaseRate)
                : max.subtract(maxPreferential == null ? BigDecimal.ZERO : maxPreferential);

        BigDecimal preferential = BigDecimal.valueOf(Math.max(0.0, memberConditionRate));
        if (maxPreferential != null) {
            preferential = preferential.min(maxPreferential);
        }

        // 데이터가 어긋나 있어도 상품이 내건 최고 금리를 넘겨서 보여 주지 않는다
        return base.add(preferential).min(max).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 계산에 쓸 가입 기간(개월).
     *
     * <p>회원이 기간을 골랐으면 그 기간을 쓰되 상품의 최장 기간을 넘기지 않는다.
     * 고르지 않았으면 최고 금리가 적용되는 기간을, 그것도 없으면 최장 기간을 쓴다.
     * 원금과 이자를 같은 기간으로 계산해야 금액이 맞는다.
     */
    public int term(int maxPeriod, Integer maxRatePeriod, Integer searchPeriod) {
        if (searchPeriod != null && searchPeriod > 0) {
            return Math.min(searchPeriod, maxPeriod);
        }
        return maxRatePeriod != null && maxRatePeriod > 0 ? maxRatePeriod : maxPeriod;
    }

    /**
     * 매월 같은 금액을 넣는 적립식 상품의 세후 만기 수령액(단리).
     *
     * <p>첫 달 납입분은 n개월, 마지막 달 납입분은 1개월 치 이자가 붙는다.
     * <pre>
     *   세전 이자 = 월 납입액 × n(n+1)/2 × 연 이율 / 12
     *   세금      = 세전 이자 × 세율
     *   수령액    = 월 납입액 × n + 세전 이자 − 세금
     * </pre>
     * 이자와 세금은 원 미만을 버린다.
     *
     * @param monthlyAmount 매월 납입액(원)
     * @param months        가입 기간(개월)
     * @param annualRate    연 이율(%)
     * @param taxRate       이자에 붙는 세율. 일반과세 0.154, 비과세 0
     */
    public MaturityEstimateDto estimate(long monthlyAmount, int months, BigDecimal annualRate, double taxRate) {
        if (monthlyAmount <= 0) {
            throw new IllegalArgumentException("월 납입액은 0보다 커야 한다: " + monthlyAmount);
        }
        if (months <= 0) {
            throw new IllegalArgumentException("가입 기간은 0보다 커야 한다: " + months);
        }
        if (annualRate == null || annualRate.signum() < 0) {
            throw new IllegalArgumentException("연 이율은 0 이상이어야 한다: " + annualRate);
        }
        if (taxRate < 0 || taxRate >= 1) {
            throw new IllegalArgumentException("세율은 0 이상 1 미만이어야 한다: " + taxRate);
        }

        long principal = monthlyAmount * months;
        // 납입 회차별로 이자가 붙는 개월 수의 합: n + (n-1) + … + 1
        long accruedMonths = (long) months * (months + 1) / 2;

        long interest = BigDecimal.valueOf(monthlyAmount)
                .multiply(BigDecimal.valueOf(accruedMonths))
                .multiply(annualRate)
                .divide(MONTHS_PER_YEAR_PERCENT, 0, RoundingMode.DOWN)
                .longValueExact();
        long tax = BigDecimal.valueOf(interest)
                .multiply(BigDecimal.valueOf(taxRate))
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();

        return MaturityEstimateDto.builder()
                .months(months)
                .appliedRate(annualRate)
                .monthlyAmount(monthlyAmount)
                .principal(principal)
                .interest(interest)
                .tax(tax)
                .total(principal + interest - tax)
                .build();
    }
}
