package com.kb04.starroad.Controller;

import com.kb04.starroad.Config.LoginMember;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.product.ProductPageResponseDto;
import com.kb04.starroad.Dto.product.ProductSearchRequestDto;
import com.kb04.starroad.Service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "예적금 상품 API")
@RestController
@RequestMapping("/api/starroad/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "예적금 상품 조회·검색",
            description = "예적금 상품을 조회할 수 있다. 조건을 주면 조건에 맞는 상품만 검색한다. "
                    + "로그인한 회원에게는 만기 예상 금액을 함께 준다")
    @GetMapping
    public ResponseEntity<ProductPageResponseDto> product(
            @ParameterObject @ModelAttribute ProductSearchRequestDto requestDto,
            @LoginMember(required = false) MemberDto loginMember) {
        return ResponseEntity.ok(productService.searchProducts(requestDto, loginMember));
    }
}
