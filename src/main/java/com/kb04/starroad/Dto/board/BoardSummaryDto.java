package com.kb04.starroad.Dto.board;

import com.kb04.starroad.Entity.Board;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/** 게시판 메인에 한 줄로 보여 줄 게시글. 본문과 이미지는 담지 않는다. */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class BoardSummaryDto {

    private final int no;
    private final String title;
    private final String detailType;
    private final int likes;

    public static BoardSummaryDto from(Board board) {
        return BoardSummaryDto.builder()
                .no(board.getNo())
                .title(board.getTitle())
                .detailType(board.getDetailType())
                .likes(board.getLikes())
                .build();
    }
}
