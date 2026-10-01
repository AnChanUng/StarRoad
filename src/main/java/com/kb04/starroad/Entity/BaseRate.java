package com.kb04.starroad.Entity;

import lombok.*;

import jakarta.persistence.*;

@Getter
@Entity
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "base_rate")
@SequenceGenerator(name = "base_rate_seq", sequenceName = "base_rate_seq", allocationSize = 1)
public class BaseRate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "base_rate_seq")
    private int no;

    @Column(name = "min_period", nullable = false)
    private int minPeriod;

    @Column(name = "max_period", nullable = false)
    private int maxPeriod;

    @Column(nullable = false)
    private Double rate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prod_no", nullable = false)
    private Product prod;

    /** 상품의 가입 기간 구간(minPeriod ~ maxPeriod 개월)에 적용되는 기본 연 이율(%) */
    public static BaseRate of(Product prod, int minPeriod, int maxPeriod, Double rate) {
        return BaseRate.builder()
                .prod(prod)
                .minPeriod(minPeriod)
                .maxPeriod(maxPeriod)
                .rate(rate)
                .build();
    }

}
