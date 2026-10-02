package com.kb04.starroad.Entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "heart")
public class Heart {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "heart_seq")
    @SequenceGenerator(name = "heart_seq", sequenceName = "HEART_SEQ", allocationSize = 1)
    @Column(name = "no")
    private int no;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_no")
    private Member member;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "board_no")
    private Board board;

    /** 회원이 게시글에 누른 좋아요 */
    public static Heart of(Member member, Board board) {
        return Heart.builder()
                .member(member)
                .board(board)
                .build();
    }
}
