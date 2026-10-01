package com.kb04.starroad;

import com.kb04.starroad.Dto.product.MaturityEstimateDto;
import com.kb04.starroad.Service.MaturityCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 만기 수령액 계산 규칙. DB·스프링 없이 돈다. */
class MaturityCalculatorTest {

    private final MaturityCalculator calculator = new MaturityCalculator();

    // ------------------------------------------------------------------ 적용 이율

    @Test
    void personalRate_withoutAnyCondition_isBaseRateNotMaxRate() {
        // 최고 연 5.00% 중 1.20%p 는 우대금리다. 조건을 하나도 못 채우면 3.80% 만 받는다.
        assertThat(calculator.personalRate(5.00, 1.20, null, 0.0)).isEqualByComparingTo("3.80");
    }

    @Test
    void personalRate_addsOnlyConditionsTheMemberMeets() {
        assertThat(calculator.personalRate(5.00, 1.20, null, 0.50)).isEqualByComparingTo("4.30");
        assertThat(calculator.personalRate(5.00, 1.20, null, 1.20)).isEqualByComparingTo("5.00");
    }

    @Test
    void personalRate_neverExceedsMaxRate() {
        // 우대 조건 금리 합이 상품의 최대 우대금리보다 커도 최고 금리를 넘지 않는다
        assertThat(calculator.personalRate(5.00, 1.20, null, 2.00)).isEqualByComparingTo("5.00");
        assertThat(calculator.personalRate(5.00, 1.20, 4.50, 1.20)).isEqualByComparingTo("5.00");
    }

    @Test
    void personalRate_productWithoutConditions_usesMaxRate() {
        assertThat(calculator.personalRate(3.80, null, null, 0.0)).isEqualByComparingTo("3.80");
    }

    @Test
    void personalRate_usesBaseRateOfChosenPeriod() {
        // 12개월 기본 금리 3.20% + 충족한 우대금리 0.50%p
        assertThat(calculator.personalRate(5.00, 1.20, 3.20, 0.50)).isEqualByComparingTo("3.70");
    }

    @Test
    void personalRate_hasNoFloatingPointNoise() {
        // double 로 5.0 - 1.2 를 하면 3.8000000000000003 이 나온다
        assertThat(calculator.personalRate(5.0, 1.2, null, 0.0).toPlainString()).isEqualTo("3.80");
        assertThat(calculator.personalRate(4.1, 0.5, null, 0.1).toPlainString()).isEqualTo("3.70");
    }

    // ------------------------------------------------------------------ 가입 기간

    @Test
    void term_defaultsToPeriodOfMaxRate() {
        assertThat(calculator.term(36, 24, null)).isEqualTo(24);
    }

    @Test
    void term_fallsBackToMaxPeriodWhenMaxRatePeriodIsMissing() {
        assertThat(calculator.term(36, null, null)).isEqualTo(36);
    }

    @Test
    void term_usesChosenPeriodButNotBeyondProductLimit() {
        assertThat(calculator.term(36, 24, 12)).isEqualTo(12);
        assertThat(calculator.term(36, 24, 60)).isEqualTo(36);
    }

    // ------------------------------------------------------------------ 수령액

    @Test
    void estimate_installmentSimpleInterest() {
        // 월 30만원 × 24개월, 연 3.80%, 일반과세
        // 세전 이자 = 300,000 × (24×25/2) × 3.8 / 1200 = 285,000
        // 세금     = 285,000 × 0.154 = 43,890
        MaturityEstimateDto e = calculator.estimate(300_000, 24, new BigDecimal("3.80"), MaturityCalculator.GENERAL_TAX_RATE);

        assertThat(e.getPrincipal()).isEqualTo(7_200_000);
        assertThat(e.getInterest()).isEqualTo(285_000);
        assertThat(e.getTax()).isEqualTo(43_890);
        assertThat(e.getTotal()).isEqualTo(7_441_110);
        assertThat(e.getMonths()).isEqualTo(24);
        assertThat(e.getAppliedRate()).isEqualByComparingTo("3.80");
    }

    @Test
    void estimate_taxFree_keepsAllInterest() {
        MaturityEstimateDto e = calculator.estimate(300_000, 24, new BigDecimal("3.80"), MaturityCalculator.TAX_FREE);

        assertThat(e.getTax()).isZero();
        assertThat(e.getTotal()).isEqualTo(7_485_000);
    }

    @Test
    void estimate_oneMonth() {
        // 한 번 넣고 한 달 뒤 찾는다: 100,000 × 1 × 6 / 1200 = 500
        MaturityEstimateDto e = calculator.estimate(100_000, 1, new BigDecimal("6.00"), MaturityCalculator.TAX_FREE);

        assertThat(e.getPrincipal()).isEqualTo(100_000);
        assertThat(e.getInterest()).isEqualTo(500);
    }

    @Test
    void estimate_zeroRate_returnsPrincipalOnly() {
        MaturityEstimateDto e = calculator.estimate(100_000, 12, BigDecimal.ZERO, MaturityCalculator.GENERAL_TAX_RATE);

        assertThat(e.getInterest()).isZero();
        assertThat(e.getTotal()).isEqualTo(1_200_000);
    }

    @Test
    void estimate_truncatesBelowOneWon() {
        // 세전 이자 = 10,000 × 6 × 3.33 / 1200 = 166.5 → 166
        // 세금     = 166 × 0.154 = 25.564 → 25
        MaturityEstimateDto e = calculator.estimate(10_000, 3, new BigDecimal("3.33"), MaturityCalculator.GENERAL_TAX_RATE);

        assertThat(e.getInterest()).isEqualTo(166);
        assertThat(e.getTax()).isEqualTo(25);
        assertThat(e.getTotal()).isEqualTo(30_000 + 166 - 25);
    }

    @Test
    void estimate_higherPersonalRateMeansMoreMoney() {
        // 같은 상품·같은 납입액이라도 우대 조건을 채운 사람이 더 받는다
        MaturityEstimateDto none = calculator.estimate(300_000, 24,
                calculator.personalRate(5.00, 1.20, null, 0.0), MaturityCalculator.GENERAL_TAX_RATE);
        MaturityEstimateDto all = calculator.estimate(300_000, 24,
                calculator.personalRate(5.00, 1.20, null, 1.20), MaturityCalculator.GENERAL_TAX_RATE);

        assertThat(all.getTotal() - none.getTotal()).isEqualTo(76_140);
    }

    @Test
    void estimate_matchesClosedFormOfLegacyScreenFormula() {
        // 예전 화면(JSP)의 식: 원금 × (1 + 이율 × (n+1)/24 × (1-세율) / 100)
        // 원금과 이자의 개월 수가 같을 때는 새 계산과 원 단위 버림 차이만 난다.
        long monthly = 250_000;
        int n = 36;
        double rate = 4.30;
        double legacy = (monthly * n) * (1 + ((rate * (n + 1) / 24) * (1 - 0.154)) / 100);

        MaturityEstimateDto e = calculator.estimate(monthly, n, new BigDecimal("4.30"), MaturityCalculator.GENERAL_TAX_RATE);

        assertThat((double) e.getTotal()).isCloseTo(legacy, org.assertj.core.data.Offset.offset(2.0));
    }

    @Test
    void estimate_rejectsInvalidInput() {
        BigDecimal rate = new BigDecimal("3.00");
        assertThatThrownBy(() -> calculator.estimate(0, 12, rate, 0.154)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.estimate(-1, 12, rate, 0.154)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.estimate(100_000, 0, rate, 0.154)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.estimate(100_000, 12, new BigDecimal("-1"), 0.154)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.estimate(100_000, 12, rate, 1.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
