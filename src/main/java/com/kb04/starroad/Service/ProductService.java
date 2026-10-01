package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.product.MaturityEstimateDto;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.product.ProductPageResponseDto;
import com.kb04.starroad.Dto.product.ProductResponseDto;
import com.kb04.starroad.Dto.product.ProductSearchRequestDto;

import com.kb04.starroad.Entity.BaseRate;
import com.kb04.starroad.Entity.Condition;
import com.kb04.starroad.Entity.MemberCondition;
import com.kb04.starroad.Repository.*;
import com.kb04.starroad.Repository.Specification.BaseRateSpecification;
import com.kb04.starroad.Repository.Specification.MemberConditionSpecification;

import com.kb04.starroad.Entity.Product;
import com.kb04.starroad.Entity.Subscription;
import com.kb04.starroad.Repository.ProductRepository;

import com.kb04.starroad.Repository.Specification.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private static final int ITEMS_PER_PAGE = 3;
    /** 검색 조건의 이자 과세 값 — 비과세 */
    private static final String RATE_TAX_FREE = "none";

    private final ProductRepository productRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MemberConditionRepository memberConditionRepository;
    private final BaseRateRepository baseRateRepository;
    private final MaturityCalculator maturityCalculator;

    public ProductService(ProductRepository productRepository, SubscriptionRepository subscriptionRepository,
                          MemberConditionRepository memberConditionRepository, BaseRateRepository baseRateRepository,
                          MaturityCalculator maturityCalculator) {
        this.maturityCalculator = maturityCalculator;
        this.productRepository = productRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.memberConditionRepository = memberConditionRepository;
        this.baseRateRepository = baseRateRepository;
    }

    /**
     * 예적금 상품 조회·검색. type·period·query 를 모두 비우면 전체를 조회한다.
     *
     * <p>로그인한 회원에게는 매월 저축할 수 있는 금액으로 가입 가능한 상품만 보여 주고,
     * 상품마다 만기 예상 금액을 계산해 채운다.
     *
     * @param loginMember 로그인한 회원. 비로그인이면 null
     */
    @Transactional
    public ProductPageResponseDto searchProducts(ProductSearchRequestDto request, MemberDto loginMember) {
        Character type = StringUtils.hasText(request.getType()) ? request.getType().charAt(0) : null;
        String period = StringUtils.hasText(request.getPeriod()) ? request.getPeriod() : null;
        String query = request.getQuery();
        boolean hasCondition = type != null || period != null || query != null;

        List<ProductResponseDto> productList;
        Double monthlyAvailablePrice = null;

        if (loginMember == null) { // 로그인 안한 경우
            productList = hasCondition ? findByForm(type, period, query) : getProductList();
        } else { // 로그인 한 경우
            monthlyAvailablePrice = getMonthlyAvailablePricePerMember(loginMember);
            productList = hasCondition
                    ? findByFormAndMember(type, period, query, monthlyAvailablePrice)
                    : getProductList(monthlyAvailablePrice);
        }

        if (period != null) {
            setBaseRate(productList, Integer.parseInt(period));
        }
        if (loginMember != null) {
            // 기간별 기본 이율(setBaseRate)을 채운 뒤에 계산해야 고른 기간의 이율이 반영된다
            double taxRate = RATE_TAX_FREE.equals(request.getRate())
                    ? MaturityCalculator.TAX_FREE : MaturityCalculator.GENERAL_TAX_RATE;
            applyEstimates(productList, monthlyAvailablePrice, getMemberConditionRates(loginMember),
                    period == null ? null : Integer.valueOf(period), taxRate);
        }

        return returnProductsByPage(productList, request.getPage(),
                loginMember == null ? null : loginMember.getName());
    }

    private ProductPageResponseDto returnProductsByPage(List<ProductResponseDto> productList, int page, String user) {
        int totalCount = productList.size();
        int startIndex = Math.min(Math.max(page - 1, 0) * ITEMS_PER_PAGE, totalCount);
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalCount);

        return ProductPageResponseDto.of(user, productList.subList(startIndex, endIndex),
                (int) Math.ceil(totalCount / (double) ITEMS_PER_PAGE), page);
    }

    /** 회원이 매월 새로 저축할 수 있는 금액(천원) = 월수입 × 저금 목표치 − 이미 납입 중인 적금 */
    private Double getMonthlyAvailablePricePerMember(MemberDto loginMember) {
        int memberSalary = loginMember.getSalary();
        int memberGoal = loginMember.getGoal();
        Double monthGoal = (1.0 * memberSalary) * (1.0 * memberGoal) / 100;

        int sum = 0; // 매달 이미 나가고 있는 적금의 양
        for (Subscription subscription : subscriptionRepository.findByMemberNo(loginMember.getNo())) {
            if (subscription.getProd().getType() == 'S') // 적금인 상품에 대해서 매달 나가는 비용 계산
                sum += subscription.getPrice();
        }
        return monthGoal - sum;
    }

    // 검색했을 때 기간 적용 -> 최대 기본 이율
    private void setBaseRate(List<ProductResponseDto> productList, int period) {
        for (BaseRate baseRate : getBaseRates(period)) {
            for (ProductResponseDto prodDto : productList) {
                if (prodDto.getNo() == baseRate.getProd().getNo())
                    prodDto.setBaseRate(baseRate.getRate());
            }
        }
    }

    public List<ProductResponseDto> makeProductResponseDtoList(List<Product> productListAll) {
        List<ProductResponseDto> list = new ArrayList<>();
        for (Product product : productListAll) {
            list.add(ProductResponseDto.from(product));
        }
        return list;
    }

    public List<ProductResponseDto> getProductList() {
        Specification<Product> spec = (root, query, criteriaBuilder) -> null;
        spec = spec.and(ProductSpecification.orderByMaxRateDescMaxRatePeriodDesc(spec));

        List<Product> productListAll = productRepository.findAll(spec);
        List<ProductResponseDto> list = makeProductResponseDtoList(productListAll);
        return list;
    }


    public List<ProductResponseDto> getProductList(Double monthlyAvailablePrice) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> null;
        spec = spec.and(ProductSpecification.lessThanOrEqualToMinPrice(monthlyAvailablePrice));
        spec = spec.and(ProductSpecification.orderByMaxRateTimesPeriodDesc(spec));

        List<Product> productListAll = productRepository.findAll(spec);
        List<ProductResponseDto> list = makeProductResponseDtoList(productListAll);
        return list;
    }

    public List<ProductResponseDto> findByForm(Character type, String period, String name) {
        Specification<Product> spec = findByFormSpec(type, period, name);
        spec = spec.and(ProductSpecification.orderByMaxRateDescMaxRatePeriodDesc(spec));

        List<Product> productListAll = productRepository.findAll(spec);
        List<ProductResponseDto> list = makeProductResponseDtoList(productListAll);

        return list;
    }

    public List<ProductResponseDto> findByFormAndMember(Character type, String period, String name, Double monthlyAvailablePrice) {

        Specification<Product> spec = findByFormSpec(type, period, name);
        spec = spec.and(ProductSpecification.lessThanOrEqualToMinPrice(monthlyAvailablePrice));
        spec = spec.and(ProductSpecification.orderByMaxRateTimesPeriodDesc(spec));

        List<Product> productListAll = productRepository.findAll(spec);
        List<ProductResponseDto> list = makeProductResponseDtoList(productListAll);

        return list;
    }

    private Specification<Product> findByFormSpec(Character type, String period, String name) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> null;
        if (name != null)
            spec = spec.and(ProductSpecification.containsName(name));
        if (period != null)
            spec = spec.and(ProductSpecification.lessThanOrEqualToMinPeriod(Integer.parseInt(period)));
        if (type != null)
            spec = spec.and(ProductSpecification.equalsType(type));
        return spec;
    }

    /** 회원이 충족한 우대 조건 */
    public List<Condition> getMemberConditions(MemberDto loginMember) {
        Specification<MemberCondition> spec = (root, query, criteriaBuilder) -> null;
        spec = spec.and(MemberConditionSpecification.equalsMemberNo(loginMember.getNo()));
        List<MemberCondition> memberConditions = memberConditionRepository.findAll(spec);

        List<Condition> result = new ArrayList<>();
        for (MemberCondition memberCondition : memberConditions) {
            result.add(memberCondition.getCondition());
        }
        return result;
    }

    /** 회원이 충족한 우대 조건의 금리를 상품 번호별로 합한다. */
    public Map<Integer, Double> getMemberConditionRates(MemberDto loginMember) {
        Map<Integer, Double> rates = new HashMap<>();
        for (Condition condition : getMemberConditions(loginMember)) {
            rates.merge(condition.getProd().getNo(), condition.getRate(), Double::sum);
        }
        return rates;
    }

    /**
     * 상품마다 회원 기준 만기 예상 금액을 계산해 채운다.
     *
     * @param monthlyAvailablePrice 회원이 매월 새로 저축할 수 있는 금액(천원). 0 이하면 계산하지 않는다.
     * @param memberConditionRates  상품 번호 → 회원이 충족한 우대금리 합
     * @param searchPeriod          회원이 고른 가입 기간(개월). 고르지 않았으면 null
     * @param taxRate               이자 세율 (일반과세 0.154, 비과세 0)
     */
    public void applyEstimates(List<ProductResponseDto> products, Double monthlyAvailablePrice,
                               Map<Integer, Double> memberConditionRates, Integer searchPeriod, double taxRate) {
        long monthlyAmount = monthlyAvailablePrice == null ? 0 : Math.round(monthlyAvailablePrice * 1000);
        for (ProductResponseDto product : products) {
            product.setEstimate(monthlyAmount <= 0 ? null
                    : estimate(product, monthlyAmount, memberConditionRates, searchPeriod, taxRate));
        }
    }

    private MaturityEstimateDto estimate(ProductResponseDto product, long monthlyAmount,
                                         Map<Integer, Double> memberConditionRates, Integer searchPeriod,
                                         double taxRate) {
        double memberRate = memberConditionRates == null ? 0.0
                : memberConditionRates.getOrDefault(product.getNo(), 0.0);
        BigDecimal rate = maturityCalculator.personalRate(
                product.getMaxRate(), product.getMaxConditionRate(), product.getBaseRate(), memberRate);
        int months = maturityCalculator.term(product.getMaxPeriod(), product.getMaxRatePeriod(), searchPeriod);
        return maturityCalculator.estimate(monthlyAmount, months, rate, taxRate);
    }

    /** 가입 기간에 적용되는 상품별 기본 이율 중 가장 높은 것 */
    public List<BaseRate> getBaseRates(int period) {
        Specification<BaseRate> spec = (root, query, criteriaBuilder) -> null;
        spec = spec.and(BaseRateSpecification.maxRateSpecification(period));
        return baseRateRepository.findAll(spec);
    }

}
