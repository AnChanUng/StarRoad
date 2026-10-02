package com.kb04.starroad.Controller;

import com.kb04.starroad.Config.LoginMemberArgumentResolver;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.auth.LoginRequestDto;
import com.kb04.starroad.Dto.auth.LoginResponseDto;
import com.kb04.starroad.Service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Tag(name = "로그인 API")
@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "로그인 기능", description = "로그인을 할 수 있다")
    @PostMapping("/api/starroad/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto requestDto, HttpSession session) {
        MemberDto memberDto = authService.authenticate(requestDto);
        session.setAttribute(LoginMemberArgumentResolver.SESSION_KEY, memberDto);
        return ResponseEntity.ok(LoginResponseDto.from(memberDto));
    }

    @Operation(summary = "로그아웃 기능", description = "로그아웃을 할 수 있다")
    @PostMapping("/api/starroad/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();  // 세션 정보를 모두 삭제
        return ResponseEntity.noContent().build();
    }
}
