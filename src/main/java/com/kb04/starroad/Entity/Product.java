package com.kb04.starroad.Entity;

import com.kb04.starroad.Dto.SubProdDto;
import com.kb04.starroad.Dto.product.ProductDto;
import com.kb04.starroad.Dto.product.ProductResponseDto;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.springframework.lang.Nullable;

import jakarta.persistence.*;

@Getter
@Entity
@Builder
@Table(name = "product")
@NoArgsConstructor
@AllArgsConstructor
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

    public ProductDto toProductDto() {
        return ProductDto.builder()
                .no(no)
                .type(type)
                .name(name)
                .explain(explain)
                .attribute(attribute)
                .minPeriod(minPeriod)
                .maxPeriod(maxPeriod)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .link(link)
                .maxRate(maxRate)
                .maxRatePeriod(maxRatePeriod)
                .maxConditionRate(maxConditionRate)
                .build();
    }

    public ProductResponseDto toProductResponseDto() {
        return ProductResponseDto.builder()
                .no(no)
                .type(type)
                .attribute(attribute)
                .name(name)
                .explain(explain)
                .maxRate(maxRate)
                .maxRatePeriod(maxRatePeriod)
                .maxConditionRate(maxConditionRate)
                .maxPeriod(maxPeriod)
                .link(link)
                .build();
    }
}