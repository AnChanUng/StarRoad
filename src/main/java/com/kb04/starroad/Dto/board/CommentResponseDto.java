package com.kb04.starroad.Dto.board;

import com.kb04.starroad.Entity.Comment;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class CommentResponseDto {

    private final int no;
    private final int boardNo;
    /** 댓글을 쓴 회원의 아이디 */
    private final String memberId;
    private final Date regdate;
    private final String content;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .no(comment.getNo())
                .boardNo(comment.getBoard().getNo())
                .memberId(comment.getMember() == null ? null : comment.getMember().getId())
                .regdate(comment.getRegdate())
                .content(comment.getContent())
                .build();
    }
}
