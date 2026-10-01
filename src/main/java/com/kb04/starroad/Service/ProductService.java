package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.product.BaseRateDto;
import com.kb04.starroad.Dto.product.ConditionDto;
import com.kb04.starroad.Dto.product.MaturityEstimateDto;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.SubProdDto;
import com.kb04.starroad.Dto.SubscriptionDto;
import com.kb04.starroad.Dto.product.ProductResponseDto;

import com.kb04.starroad.Entity.BaseRate;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

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

    public List<ProductResponseDto> makeProductResponseDtoList(List<Product> productListAll) {
        List<ProductResponseDto> list = new ArrayList<>();
        for (Product product : productListAll) {
            ProductResponseDto dto = product.toProductResponseDto();
            list.add(dto);
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

    public List<SubscriptionDto> getSubscriptions(MemberDto memberDto) {
        return subscriptionRepository.findByMember(memberDto.toMemberEntity()).stream().map(Subscription::toSubscriptionDto).collect(Collectors.toList());
    }

    public List<ConditionDto> getMemberConditions(MemberDto loginMember) {
        Specification<MemberCondition> spec = (root, query, criteriaBuilder) -> null;
        spec = spec.and(MemberConditionSpecification.equalsMemberNo(loginMember.toMemberEntity()));
        List<MemberCondition> memberConditions = memberConditionRepository.findAll(spec);

        List<ConditionDto> result = new ArrayList<>();
        for (MemberCondition memberCondition : memberConditions) {
            result.add(memberCondition.getCondition().toConditionDto());
        }
        return result;
    }

    /** 회원이 충족한 우대 조건의 금리를 상품 번호별로 합한다. */
    public Map<Integer, Double> getMemberConditionRates(MemberDto loginMember) {
        Map<Integer, Double> rates = new HashMap<>();
        for (ConditionDto condition : getMemberConditions(loginMember)) {
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

    public List<BaseRateDto> getBaseRates(int period) {
        Specification<BaseRate> spec = (root, query, criteriaBuilder) -> null;
        spec = spec.and(BaseRateSpecification.maxRateSpecification(period));
        List<BaseRate> baseRates = baseRateRepository.findAll(spec);
        List<BaseRateDto> list = new ArrayList<>();
        for (BaseRate baseRate : baseRates) {
            BaseRateDto dto = baseRate.toBaseRateDto();
            list.add(dto);
        }
        return list;
    }

    public SubProdDto getProductInfo(String sub_name) {
        Specification<Subscription> spec = (root, query, criteriaBuilder) -> null;
        spec = spec.and(ProductSpecification.getProdInfo(productRepository.findOneByName(sub_name)));
        return subscriptionRepository.findOne(spec).map(Subscription::toSubProdDto).orElse(new SubProdDto());
    }

}
