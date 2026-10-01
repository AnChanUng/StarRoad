package com.kb04.starroad.Dto.board;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 게시판 메인 화면 — 게시판별 최신 글 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardMainResponseDto {

    private final List<BoardSummaryDto> popularBoard;
    private final List<BoardSummaryDto> freeBoard;
    private final List<BoardSummaryDto> authBoard;

    public static BoardMainResponseDto of(List<BoardSummaryDto> popularBoard,
                                          List<BoardSummaryDto> freeBoard,
                                          List<BoardSummaryDto> authBoard) {
        return new BoardMainResponseDto(popularBoard, freeBoard, authBoard);
    }
}
