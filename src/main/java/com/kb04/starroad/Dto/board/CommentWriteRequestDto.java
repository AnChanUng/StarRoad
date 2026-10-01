package com.kb04.starroad.Dto.board;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 댓글 작성 요청 */
@Getter
@Setter
@NoArgsConstructor
public class CommentWriteRequestDto {

    /** 댓글을 달 게시글 번호 */
    private int boardNo;
    private String content;
}
