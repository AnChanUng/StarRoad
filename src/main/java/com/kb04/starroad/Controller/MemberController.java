package com.kb04.starroad.Controller;


import com.kb04.starroad.Dto.member.DuplicateCheckResponseDto;
import com.kb04.starroad.Dto.member.MemberJoinRequestDto;
import com.kb04.starroad.Dto.member.MemberResponseDto;
import com.kb04.starroad.Service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/starroad/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입", description = "회원가입을 할 수 있다")
    @PostMapping
    public ResponseEntity<MemberResponseDto> join(@RequestBody MemberJoinRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.join(requestDto));
    }

    // 아이디 중복 확인을 위한 엔드포인트
    @Operation(summary = "회원아이디 중복체크", description = "회원아이디 중복체크 할 수 있다")
    @GetMapping("/check-id")
    public ResponseEntity<DuplicateCheckResponseDto> checkId(
            @Parameter(description = "회원아이디") @RequestParam("id") String id) {
        return ResponseEntity.ok(memberService.checkId(id));
    }

    @Operation(summary = "회원이메일 중복체크", description = "회원이메일 중복체크 할 수 있다")
    @GetMapping("/check-email")
    public ResponseEntity<DuplicateCheckResponseDto> checkEmail(
            @Parameter(description = "회원이메일") @RequestParam("email") String email) {
        return ResponseEntity.ok(memberService.checkEmail(email));
    }

}
