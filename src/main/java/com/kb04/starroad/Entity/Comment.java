package com.kb04.starroad.Entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_seq")
    @SequenceGenerator(name = "comment_seq", sequenceName = "COMMENT_SEQ", allocationSize = 1)
    @Column(name = "no")
    private int no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_no")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_no")
    private Member member;

    @Column(name = "regdate", nullable = false)
    private Date regdate;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(columnDefinition = "char(1)  default 'Y'", name = "status", nullable = false)
    private Character status;

    /** 회원이 게시글에 댓글을 단다. 작성 시각은 지금, 상태는 게시('Y')다. */
    public static Comment write(Board board, Member writer, String content) {
        return Comment.builder()
                .board(board)
                .member(writer)
                .content(content)
                .regdate(new Date())
                .status('Y')
                .build();
    }

    public void update(String content) {
        this.content = content;
    }

    /** 이 댓글을 쓴 회원의 아이디가 맞는지 */
    public boolean isWrittenBy(String memberId) {
        return member != null && member.getId().equals(memberId);
    }

}
