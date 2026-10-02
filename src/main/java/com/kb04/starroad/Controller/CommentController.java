package com.kb04.starroad.Controller;

import com.kb04.starroad.Config.LoginMember;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.board.CommentResponseDto;
import com.kb04.starroad.Dto.board.CommentUpdateRequestDto;
import com.kb04.starroad.Dto.board.CommentWriteRequestDto;
import com.kb04.starroad.Service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "댓글 API")
@RestController
@RequestMapping("/api/starroad/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Operation(summary = "댓글 생성", description = "댓글을 생성 할 수 있다")
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @LoginMember MemberDto loginMember,
            @RequestBody CommentWriteRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.writeComment(loginMember.getNo(), requestDto));
    }

    // 조회
    @Operation(summary = "댓글 조회", description = "댓글을 조회 할 수 있다")
    @GetMapping("/{no}")
    public ResponseEntity<CommentResponseDto> getComment(
            @Parameter(description = "댓글 번호", example = "1") @PathVariable("no") int commentNo) {
        return ResponseEntity.ok(commentService.getComment(commentNo));
    }

    @Operation(summary = "댓글 수정 처리", description = "댓글을 수정 내용을 처리하고 DB에 업데이트합니다")
    @PutMapping("/{no}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @Parameter(description = "수정할 댓글 번호", example = "1") @PathVariable("no") int commentNo,
            @LoginMember MemberDto loginMember,
            @RequestBody CommentUpdateRequestDto requestDto) {
        return ResponseEntity.ok(commentService.updateComment(commentNo, loginMember.getId(), requestDto));
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제 할 수 있다")
    @DeleteMapping("/{no}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "삭제할 댓글 번호", example = "1") @PathVariable("no") int commentNo,
            @LoginMember MemberDto loginMember) {
        commentService.deleteComment(commentNo, loginMember.getId());
        return ResponseEntity.noContent().build();
    }

}
