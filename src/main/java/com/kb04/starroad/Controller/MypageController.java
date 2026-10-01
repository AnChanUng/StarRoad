package com.kb04.starroad.Controller;

import com.kb04.starroad.Config.LoginMember;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.board.BoardResponseDto;
import com.kb04.starroad.Dto.member.MemberResponseDto;
import com.kb04.starroad.Dto.mypage.ChallengeResponseDto;
import com.kb04.starroad.Dto.mypage.MemberUpdateRequestDto;
import com.kb04.starroad.Dto.mypage.MyCommentResponseDto;
import com.kb04.starroad.Dto.mypage.MypageResponseDto;
import com.kb04.starroad.Dto.mypage.PasswordCheckResponseDto;
import com.kb04.starroad.Dto.mypage.PasswordRequestDto;
import com.kb04.starroad.Dto.mypage.RewardResponseDto;
import com.kb04.starroad.Service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "마이페이지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/starroad/mypage")
public class MypageController {

    private final MemberService memberService;

    @Operation(summary = "자산", description = "자신의 자산을 확인할 수 있습니다")
    @GetMapping("/asset")
    public ResponseEntity<MypageResponseDto> asset(@LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(memberService.getAssets(loginMember.getNo()));
    }

    @Operation(summary = "나의 게시판", description = "나의 게시물을 확인할 수 있습니다")
    @GetMapping("/boards")
    public ResponseEntity<List<BoardResponseDto>> board(@LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(memberService.getWritings(loginMember.getNo()));
    }

    @Operation(summary = "나의 댓글", description = "나의 댓글을 확인할 수 있습니다")
    @GetMapping("/comments")
    public ResponseEntity<List<MyCommentResponseDto>> comment(@LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(memberService.getComments(loginMember.getNo()));
    }

    @Operation(summary = "나의 챌린지", description = "나의 챌린지를 확인할 수 있습니다")
    @GetMapping("/challenges")
    public ResponseEntity<List<ChallengeResponseDto>> challenge(@LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(memberService.getChallenges(loginMember.getNo()));
    }

    @Operation(summary = "포인트리 확인", description = "가입한 상품을 완주하면 받을 포인트리를 확인할 수 있습니다")
    @GetMapping("/challenges/{subNo}/reward")
    public ResponseEntity<RewardResponseDto> reward(
            @Parameter(description = "가입상품번호") @PathVariable("subNo") int subNo,
            @LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(memberService.getReward(loginMember.getNo(), subNo));
    }

    @Operation(summary = "포인트리 받기", description = "완주한 상품의 포인트리를 받을 수 있습니다")
    @PostMapping("/challenges/{subNo}/reward")
    public ResponseEntity<RewardResponseDto> receiveReward(
            @Parameter(description = "가입상품번호") @PathVariable("subNo") int subNo,
            @LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(memberService.receiveReward(loginMember.getNo(), subNo));
    }

    @Operation(summary = "나의정보 확인", description = "나의 정보를 확인할 수 있습니다")
    @GetMapping("/info")
    public ResponseEntity<MemberResponseDto> info(@LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(memberService.getInfo(loginMember.getNo()));
    }

    //회원정보 수정하는 부분
    @Operation(summary = "나의정보 수정", description = "나의 정보를 수정 할 수 있습니다")
    @PutMapping("/info")
    public ResponseEntity<MemberResponseDto> updateInfo(
            @LoginMember MemberDto loginMember,
            @RequestBody MemberUpdateRequestDto changeDto) {
        return ResponseEntity.ok(memberService.memberUpdate(loginMember, changeDto));
    }

    @Operation(summary = "나의정보 비밀번호 수정", description = "나의 비밀번호를 수정 할 수 있습니다")
    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @LoginMember MemberDto loginMember,
            @RequestBody PasswordRequestDto requestDto) {
        memberService.memberPasswordUpdate(loginMember, requestDto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "나의 비밀번호 확인", description = "나의 비밀번호를 확인 할 수 있습니다")
    @PostMapping("/check-password")
    public ResponseEntity<PasswordCheckResponseDto> checkPassword(
            @LoginMember MemberDto loginMember,
            @RequestBody PasswordRequestDto requestDto) {
        return ResponseEntity.ok(memberService.checkPassword(loginMember.getNo(), requestDto));
    }
}
