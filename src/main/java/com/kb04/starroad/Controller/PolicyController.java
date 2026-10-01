package com.kb04.starroad.Controller;

import com.kb04.starroad.Config.LoginMember;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.policy.PolicyLikeResponseDto;
import com.kb04.starroad.Dto.policy.PolicyPageResponseDto;
import com.kb04.starroad.Dto.policy.PolicyRequestDto;
import com.kb04.starroad.Service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "청년정책 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/starroad/policies")
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "청년정책 조회·검색", description = "청년정책을 조회할 수 있다. 조건을 주면 조건에 맞는 정책만 검색한다")
    @GetMapping
    public ResponseEntity<PolicyPageResponseDto> policy(
            @ParameterObject @ModelAttribute PolicyRequestDto requestDto,
            @LoginMember(required = false) MemberDto loginMember) {
        return ResponseEntity.ok(policyService.searchPolicies(requestDto, loginMember));
    }

    @Operation(summary = "청년정책 찜", description = "청년정책을 관심 정책으로 등록할 수 있다. 이미 등록한 정책이면 해제한다")
    @PostMapping("/{policyNo}/like")
    public ResponseEntity<PolicyLikeResponseDto> likePolicy(
            @Parameter(description = "정책 번호", example = "1") @PathVariable("policyNo") int policyNo,
            @LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(policyService.togglePolicyHeart(loginMember, policyNo));
    }

}
