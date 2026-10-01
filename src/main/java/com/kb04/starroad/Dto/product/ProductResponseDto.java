package com.kb04.starroad.Dto.product;

import com.kb04.starroad.Entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private int no;
    private Character type;
    private String attribute;

    private String name;
    private String explain;

    private Integer maxPrice;

    private Double maxRate;
    private Integer maxRatePeriod;
    private Double maxConditionRate;

    private int maxPeriod;
    private Double baseRate;

    private String link;

    /** 로그인한 회원 기준 만기 예상 금액. 비로그인이거나 납입 가능액이 없으면 null */
    private MaturityEstimateDto estimate;

    public static ProductResponseDto from(Product product) {
        return ProductResponseDto.builder()
                .no(product.getNo())
                .type(product.getType())
                .attribute(product.getAttribute())
                .name(product.getName())
                .explain(product.getExplain())
                .maxRate(product.getMaxRate())
                .maxRatePeriod(product.getMaxRatePeriod())
                .maxConditionRate(product.getMaxConditionRate())
                .maxPeriod(product.getMaxPeriod())
                .link(product.getLink())
                .build();
    }

}
