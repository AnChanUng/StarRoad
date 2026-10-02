package com.kb04.starroad.Dto.board;

import com.kb04.starroad.Entity.Board;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Base64;
import java.util.Date;
import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class BoardResponseDto {
    private final int no;
    private final String title;
    private final Date regdate;
    private final String content;
    private final int likes;
    private final int commentNum;
    private final String type;
    private final String detailType;
    /** 첨부 이미지(Base64). 없으면 null */
    private final String imageBase64;
    private final String memberId;
    /** 상세 조회에서만 채운다. 목록에서는 null */
    private final List<CommentResponseDto> comments;

    /** 목록용 — 댓글 없이 */
    public static BoardResponseDto from(Board board) {
        return of(board, null);
    }

    /** 상세용 — 댓글과 함께 */
    public static BoardResponseDto of(Board board, List<CommentResponseDto> comments) {
        return BoardResponseDto.builder()
                .no(board.getNo())
                .title(board.getTitle())
                .regdate(board.getRegdate())
                .content(board.getContent())
                .likes(board.getLikes())
                .commentNum(board.getCommentNum())
                .type(board.getType())
                .detailType(board.getDetailType())
                .imageBase64(board.getImage() == null ? null : Base64.getEncoder().encodeToString(board.getImage()))
                .memberId(board.getMember() == null ? null : board.getMember().getId())
                .comments(comments)
                .build();
    }
}
