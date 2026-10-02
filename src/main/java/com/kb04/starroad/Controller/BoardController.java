package com.kb04.starroad.Controller;

import com.kb04.starroad.Config.LoginMember;
import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.board.BoardMainResponseDto;
import com.kb04.starroad.Dto.board.BoardResponseDto;
import com.kb04.starroad.Dto.board.BoardUpdateRequestDto;
import com.kb04.starroad.Dto.board.BoardWriteRequestDto;
import com.kb04.starroad.Dto.board.LikeResponseDto;
import com.kb04.starroad.Service.BoardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "게시판 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/starroad/boards")
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "게시판 메인", description = "인기글, 자유게시판, 인증방의 최신 글을 보여줍니다")
    @GetMapping("/main")
    public ResponseEntity<BoardMainResponseDto> boardMain() {
        return ResponseEntity.ok(boardService.getMain());
    }

    @Operation(summary = "자유,인증 게시판", description = "자유,인증 게시판의 글 목록을 볼 수 있습니다.")
    @GetMapping
    public ResponseEntity<List<BoardResponseDto>> boardList(
            @Parameter(description = "게시글타입") @RequestParam(name = "type", defaultValue = "F") String type) {
        return ResponseEntity.ok(boardService.selectBoardAllOrderByDate(type));
    }

    @Operation(summary = "인기게시판", description = "인기게시판을 보여줍니다")
    @GetMapping("/popular")
    public ResponseEntity<List<BoardResponseDto>> popularBoardList() {
        return ResponseEntity.ok(boardService.selectPopularBoard());
    }

    @Operation(summary = "게시글 상세", description = "게시글을 상세를 볼 수 있습니다")
    @GetMapping("/{no}")
    public ResponseEntity<BoardResponseDto> getBoardDetail(
            @Parameter(description = "게시글 번호") @PathVariable("no") int no) {
        return ResponseEntity.ok(boardService.detailBoard(no));
    }

    @Operation(summary = "게시글 작성", description = "게시글을 작성할 수 있습니다")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BoardResponseDto> boardWrite(
            @LoginMember MemberDto loginMember,
            @ModelAttribute BoardWriteRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.writeBoard(loginMember.getId(), requestDto));
    }

    @Operation(summary = "게시글 수정 기능", description = "게시글을 수정할 수 있습니다")
    @PutMapping(value = "/{no}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BoardResponseDto> updateBoard(
            @Parameter(description = "게시글 번호") @PathVariable("no") int no,
            @LoginMember MemberDto loginMember,
            @ModelAttribute BoardUpdateRequestDto requestDto) {
        return ResponseEntity.ok(boardService.updateBoard(no, loginMember.getId(), requestDto));
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제할 수 있습니다")
    @DeleteMapping("/{no}")
    public ResponseEntity<Void> deleteBoard(
            @Parameter(description = "게시글 번호") @PathVariable("no") int no,
            @LoginMember MemberDto loginMember) {
        boardService.deleteBoard(no, loginMember.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 추천", description = "게시글을 추천할 수 있습니다")
    @PostMapping("/{no}/likes")
    public ResponseEntity<LikeResponseDto> handleLike(
            @Parameter(description = "게시글 번호") @PathVariable("no") int no,
            @LoginMember MemberDto loginMember) {
        return ResponseEntity.ok(boardService.increaseLikes(no, loginMember.getId()));
    }
}
