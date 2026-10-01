package com.kb04.starroad.Dto.board;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 댓글 수정 요청 */
@Getter
@Setter
@NoArgsConstructor
public class CommentUpdateRequestDto {

    private String content;
}
