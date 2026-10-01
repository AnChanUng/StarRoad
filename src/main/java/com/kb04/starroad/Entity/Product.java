package com.kb04.starroad.Entity;

import lombok.*;
import org.hibernate.annotations.Formula;
import org.springframework.lang.Nullable;

import jakarta.persistence.*;

@Getter
@Entity
@Builder(access = AccessLevel.PRIVATE)
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SequenceGenerator(name = "product_seq", sequenceName = "product_seq", allocationSize = 50, initialValue = 1)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @Column(nullable = false)
    private int no;

    @Column(columnDefinition = "char(1)", nullable = false)
    private Character type;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 1000, nullable = false)
    private String explain;

    @Column(length = 50, nullable = false)
    private String attribute;

    @Column(name = "min_period", nullable = false)
    private int minPeriod;

    @Column(name = "max_period", nullable = false)
    private int maxPeriod;

    @Column(name = "min_price", nullable = false)
    private int minPrice;

    @Nullable
    @Column(name = "max_price")
    private Integer maxPrice;

    @Column(length = 5000, nullable = false)
    private String link;

    @Column(name = "max_rate", nullable = false)
    private Double maxRate;

    @Nullable
    @Column(name = "max_rate_period")
    private Integer maxRatePeriod;

    @Nullable
    @Column(name = "max_condition_rate")
    private Double maxConditionRate;

    @Formula("(max_period * 1000) * (1 + ((max_rate - nvl(max_condition_rate, 0)) * (nvl(max_rate_period, max_period) + 1) / 24) * (1 - 0.154) / 100) ")
    private Double maxRateTimesPeriod;

    /**
     * 예적금 상품 한 건.
     *
     * @param type             'S' = 적금, 'D' = 예금
     * @param minPeriod        최소 가입 기간(개월)
     * @param maxPeriod        최장 가입 기간(개월)
     * @param maxPrice         최대 납입액. 한도가 없으면 null
     * @param maxRate          최고 연 이율(%). 우대금리를 모두 받았을 때의 값
     * @param maxRatePeriod    최고 금리가 적용되는 기간(개월). 따로 없으면 null
     * @param maxConditionRate 최고 금리 중 우대금리가 차지하는 몫(%p). 우대 조건이 없으면 null
     */
    public static Product of(Character type, String name, String attribute, String explain,
                             int minPeriod, int maxPeriod, int minPrice, Integer maxPrice,
                             Double maxRate, Integer maxRatePeriod, Double maxConditionRate,
                             String link) {
        return Product.builder()
                .type(type)
                .name(name)
                .attribute(attribute)
                .explain(explain)
                .minPeriod(minPeriod)
                .maxPeriod(maxPeriod)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .maxRate(maxRate)
                .maxRatePeriod(maxRatePeriod)
                .maxConditionRate(maxConditionRate)
                .link(link)
                .build();
    }
}
