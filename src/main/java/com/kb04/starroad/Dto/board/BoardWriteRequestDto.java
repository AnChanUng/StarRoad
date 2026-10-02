package com.kb04.starroad.Dto.board;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** 게시글 작성 요청 (multipart/form-data) */
@Getter
@Setter
@NoArgsConstructor
public class BoardWriteRequestDto {

    /** 게시판 종류 (F: 자유게시판, C: 인증방) */
    private String type;
    /** 말머리 */
    private String detailType;
    private String title;
    private String content;
    /** 첨부 이미지. 없어도 된다 */
    private MultipartFile image;
}
