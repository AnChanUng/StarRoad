package com.kb04.starroad;

import com.kb04.starroad.Dto.product.ProductResponseDto;
import com.kb04.starroad.Entity.Product;
import com.kb04.starroad.Repository.BaseRateRepository;
import com.kb04.starroad.Repository.MemberConditionRepository;
import com.kb04.starroad.Repository.ProductRepository;
import com.kb04.starroad.Repository.SubscriptionRepository;
import com.kb04.starroad.Service.MaturityCalculator;
import com.kb04.starroad.Service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 상품 목록에 회원별 만기 예상 금액을 채우는 흐름.
 * 저장소는 쓰지 않으므로 가짜로 두고, 상품 → 화면용 DTO → 계산까지를 본다.
 */
class ProductEstimateTest {

    private static final double TAX = MaturityCalculator.GENERAL_TAX_RATE;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(mock(ProductRepository.class), mock(SubscriptionRepository.class),
                mock(MemberConditionRepository.class), mock(BaseRateRepository.class), new MaturityCalculator());
    }

    @Test
    void sameProduct_differentMembers_getDifferentAmounts() {
        // 월 30만원을 넣을 수 있는 두 회원. 한 명만 우대 조건(0.50%p)을 채웠다.
        List<ProductResponseDto> forPlain = dtos(youthSavings());
        List<ProductResponseDto> forPreferred = dtos(youthSavings());

        productService.applyEstimates(forPlain, 300.0, Map.of(), null, TAX);
        productService.applyEstimates(forPreferred, 300.0, Map.of(1, 0.50), null, TAX);

        assertThat(forPlain.get(0).getEstimate().getAppliedRate()).isEqualByComparingTo("3.80");
        assertThat(forPlain.get(0).getEstimate().getTotal()).isEqualTo(7_441_110);
        assertThat(forPreferred.get(0).getEstimate().getAppliedRate()).isEqualByComparingTo("4.30");
        assertThat(forPreferred.get(0).getEstimate().getTotal()).isEqualTo(7_472_835);
    }

    @Test
    void maxConditionRate_reachesTheScreenDto() {
        // 이 값이 화면용 DTO 로 넘어오지 않으면 기본 금리를 구할 수 없어 최고 금리로 계산된다
        assertThat(youthSavings().toProductResponseDto().getMaxConditionRate()).isEqualTo(1.20);
    }

    @Test
    void usesPeriodOfMaxRate_forBothPrincipalAndInterest() {
        List<ProductResponseDto> products = dtos(youthSavings());

        productService.applyEstimates(products, 300.0, Map.of(), null, TAX);

        // 최장 36개월이지만 최고 금리는 24개월 기준이다. 원금도 24개월 치여야 한다.
        assertThat(products.get(0).getEstimate().getMonths()).isEqualTo(24);
        assertThat(products.get(0).getEstimate().getPrincipal()).isEqualTo(300_000L * 24);
    }

    @Test
    void productWithoutMaxRatePeriod_stillEarnsInterestForWholeTerm() {
        List<ProductResponseDto> products = dtos(timeDeposit());

        productService.applyEstimates(products, 300.0, Map.of(), null, TAX);

        assertThat(products.get(0).getEstimate().getMonths()).isEqualTo(36);
        assertThat(products.get(0).getEstimate().getAppliedRate()).isEqualByComparingTo("3.80");
        assertThat(products.get(0).getEstimate().getInterest()).isEqualTo(632_700);
    }

    @Test
    void chosenPeriod_usesBaseRateOfThatPeriod() {
        List<ProductResponseDto> products = dtos(youthSavings());
        products.get(0).setBaseRate(3.20);

        productService.applyEstimates(products, 300.0, Map.of(1, 0.50), 12, TAX);

        assertThat(products.get(0).getEstimate().getMonths()).isEqualTo(12);
        assertThat(products.get(0).getEstimate().getAppliedRate()).isEqualByComparingTo("3.70");
    }

    @Test
    void taxFree_paysMoreThanGeneralTax() {
        List<ProductResponseDto> taxed = dtos(youthSavings());
        List<ProductResponseDto> taxFree = dtos(youthSavings());

        productService.applyEstimates(taxed, 300.0, Map.of(), null, TAX);
        productService.applyEstimates(taxFree, 300.0, Map.of(), null, MaturityCalculator.TAX_FREE);

        assertThat(taxFree.get(0).getEstimate().getTotal() - taxed.get(0).getEstimate().getTotal()).isEqualTo(43_890);
    }

    @Test
    void conditionsOfOtherProducts_areIgnored() {
        List<ProductResponseDto> products = dtos(youthSavings());

        productService.applyEstimates(products, 300.0, Map.of(99, 1.00), null, TAX);

        assertThat(products.get(0).getEstimate().getAppliedRate()).isEqualByComparingTo("3.80");
    }

    @Test
    void noMoneyLeftToSave_noEstimate() {
        // 이미 저축 목표보다 많이 납입 중이면 음수 금액을 보여 주지 않는다
        List<ProductResponseDto> products = dtos(youthSavings());

        productService.applyEstimates(products, -50.0, Map.of(), null, TAX);
        assertThat(products.get(0).getEstimate()).isNull();

        productService.applyEstimates(products, 0.0, Map.of(), null, TAX);
        assertThat(products.get(0).getEstimate()).isNull();
    }

    // ------------------------------------------------------------------ helpers

    private static List<ProductResponseDto> dtos(Product... products) {
        List<ProductResponseDto> list = new ArrayList<>();
        for (Product product : products) {
            list.add(product.toProductResponseDto());
        }
        return list;
    }

    /** 최고 연 5.00% (24개월 기준), 그중 우대금리 최대 1.20%p */
    private static Product youthSavings() {
        return Product.builder()
                .no(1).type('S').name("KB청년희망적금").explain("청년 우대 적금").attribute("청년우대")
                .minPeriod(12).maxPeriod(36).minPrice(10000).maxPrice(500000)
                .maxRate(5.00).maxRatePeriod(24).maxConditionRate(1.20)
                .link("https://example.com/youth").build();
    }

    /** 우대 조건도, 최고 금리 기간도 없는 상품 */
    private static Product timeDeposit() {
        return Product.builder()
                .no(2).type('D').name("KB Star 정기예금").explain("기본 정기예금").attribute("일반")
                .minPeriod(6).maxPeriod(36).minPrice(1000000)
                .maxRate(3.80)
                .link("https://example.com/deposit").build();
    }
}
