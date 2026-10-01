package com.kb04.starroad.Dto.board;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** 게시글 수정 요청 (multipart/form-data) */
@Getter
@Setter
@NoArgsConstructor
public class BoardUpdateRequestDto {

    private String title;
    private String content;
    /** 새 이미지. 보내지 않으면 기존 이미지를 유지한다 */
    private MultipartFile newImage;
}
