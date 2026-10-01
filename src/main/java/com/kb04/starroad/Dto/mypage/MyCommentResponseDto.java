package com.kb04.starroad.Dto.mypage;

import com.kb04.starroad.Entity.Comment;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

/** 마이페이지 — 내가 쓴 댓글과 그 댓글이 달린 게시글 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class MyCommentResponseDto {

    private final int no;
    private final String content;
    private final Date regdate;
    private final int boardNo;
    private final String boardTitle;
    /** 게시판 종류 (F: 자유게시판, C: 인증방) */
    private final String boardType;

    public static MyCommentResponseDto from(Comment comment) {
        return MyCommentResponseDto.builder()
                .no(comment.getNo())
                .content(comment.getContent())
                .regdate(comment.getRegdate())
                .boardNo(comment.getBoard().getNo())
                .boardTitle(comment.getBoard().getTitle())
                .boardType(comment.getBoard().getType())
                .build();
    }
}
