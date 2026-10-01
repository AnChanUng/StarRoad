package com.kb04.starroad.Controller;

import com.kb04.starroad.Config.LoginMember;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.policy.PolicyNoticeResponseDto;
import com.kb04.starroad.Service.PolicyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈 API")
@RequiredArgsConstructor
@RestController
public class HomeController {

    private final PolicyService policyService;

    @Operation(summary = "관심 정책 알림", description = "홈 알림창에 보여 줄, 마감이 가장 가까운 관심 정책")
    @GetMapping("/api/starroad/home/policy-notice")
    public ResponseEntity<PolicyNoticeResponseDto> policyNotice(@LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(policyService.modalPolicy(loginMember));
    }

}
