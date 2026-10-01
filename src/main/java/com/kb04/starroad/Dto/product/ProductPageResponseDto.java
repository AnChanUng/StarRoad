package com.kb04.starroad.Dto.product;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 예적금 상품 목록 한 페이지 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductPageResponseDto {

    /** 로그인한 회원 이름. 비로그인이면 null — 이때는 상품에 만기 예상 금액이 없다 */
    private final String user;
    private final List<ProductResponseDto> productItems;
    /** 마지막 페이지 번호 */
    private final int pageEndIndex;
    private final int currentPage;

    public static ProductPageResponseDto of(String user, List<ProductResponseDto> productItems,
                                            int pageEndIndex, int currentPage) {
        return new ProductPageResponseDto(user, productItems, pageEndIndex, currentPage);
    }
}
