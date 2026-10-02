package com.kb04.starroad.Entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "board_seq")
    @SequenceGenerator(name = "board_seq", sequenceName = "BOARD_SEQ", allocationSize = 1)
    @Column(name = "no")
    private int no;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_no")
    private Member member;

    @Column(name = "title", length = 50, nullable = false)
    private String title;

    @Column(name = "regdate", nullable = false)
    private Date regdate;

    @Column(name = "content", length = 2000, nullable = false)
    private String content;

    @Column(name = "likes")
    private int likes;

    @Column(name = "comment_num", nullable = false)
    private int commentNum;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private Character status = 'Y';

    @Column(name = "type", length = 1, nullable = false)
    private String type;

    @Lob
    @Column(name = "image")
    private byte[] image;

    @Column(name = "detail_type", length = 100, nullable = false)
    private String detailType;

    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Heart> hearts = new ArrayList<>();

    /**
     * 회원이 새 게시글을 쓴다. 좋아요와 댓글 수는 0, 상태는 게시('Y')로 시작한다.
     *
     * @param type       게시판 종류 (F: 자유게시판, C: 인증방)
     * @param detailType 말머리
     * @param image      첨부 이미지. 없으면 null
     */
    public static Board write(Member writer, String type, String detailType,
                              String title, String content, byte[] image) {
        return Board.builder()
                .member(writer)
                .type(type)
                .detailType(detailType)
                .title(title)
                .content(content)
                .image(image)
                .likes(0)
                .commentNum(0)
                .status('Y')
                .build();
    }

    @PrePersist
    protected void onCreate() {
        regdate = new Date(); // 현재 날짜와 시간을 설정

        if (status == null) { // status 필드가 null인 경우 '1'로 초기화
            status = '1';
        }
    }

    /**
     * 게시물 수정 메서드
     * @param title 게시물 제목
     * @param content 게시물 내용
     * @param image 게시물 이미지
     */
    public void update(String title, String content, byte[] image){
        this.title = title;
        this.content = content;
        this.image = image;
    }

    /** 이 게시글을 쓴 회원의 아이디가 맞는지 */
    public boolean isWrittenBy(String memberId) {
        return member != null && member.getId().equals(memberId);
    }

    public void increaseLikes() {
        this.likes++;
    }

    public void increaseCommentNum() {
        this.commentNum++;
    }

    public void decreaseCommentNum() {
        this.commentNum--;
    }
}
