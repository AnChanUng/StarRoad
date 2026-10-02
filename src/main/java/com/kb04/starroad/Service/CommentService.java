package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.board.CommentResponseDto;
import com.kb04.starroad.Dto.board.CommentUpdateRequestDto;
import com.kb04.starroad.Dto.board.CommentWriteRequestDto;
import com.kb04.starroad.Entity.Board;
import com.kb04.starroad.Entity.Comment;
import com.kb04.starroad.Exception.ErrorCode;
import com.kb04.starroad.Exception.StarroadException;
import com.kb04.starroad.Repository.BoardRepository;
import com.kb04.starroad.Repository.CommentRepository;
import com.kb04.starroad.Repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    /** 게시글에 달린 댓글을 작성 순으로 */
    @Transactional
    public List<CommentResponseDto> findByBoard(Board board) {
        return commentRepository.findByBoardOrderByRegdate(board).stream()
                .map(CommentResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 작성. 게시글의 댓글 수도 함께 늘린다.
     * @param memberNo 로그인한 회원 번호
     */
    @Transactional
    public CommentResponseDto writeComment(int memberNo, CommentWriteRequestDto request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new StarroadException(ErrorCode.COMMENT_CONTENT_REQUIRED);
        }
        Board board = boardRepository.findByNo(request.getBoardNo());
        if (board == null) {
            throw new StarroadException(ErrorCode.BOARD_NOT_FOUND);
        }

        Comment comment = commentRepository.save(
                Comment.write(board, memberRepository.findByNo(memberNo), request.getContent()));
        board.increaseCommentNum();

        return CommentResponseDto.from(comment);
    }

    @Transactional
    public CommentResponseDto getComment(int commentNo) {
        return CommentResponseDto.from(findComment(commentNo));
    }

    /**
     * 댓글 수정. 작성자 본인만 할 수 있다.
     * @param memberId 로그인한 회원 아이디
     */
    @Transactional
    public CommentResponseDto updateComment(int commentNo, String memberId, CommentUpdateRequestDto request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new StarroadException(ErrorCode.COMMENT_CONTENT_REQUIRED);
        }
        Comment comment = findComment(commentNo);
        if (!comment.isWrittenBy(memberId)) {
            throw new StarroadException(ErrorCode.COMMENT_UPDATE_FORBIDDEN);
        }
        comment.update(request.getContent());
        return CommentResponseDto.from(comment);
    }

    /**
     * 댓글 삭제. 작성자 본인만 할 수 있다. 게시글의 댓글 수도 함께 줄인다.
     * @param memberId 로그인한 회원 아이디
     */
    @Transactional
    public void deleteComment(int commentNo, String memberId) {
        Comment comment = findComment(commentNo);
        if (!comment.isWrittenBy(memberId)) {
            throw new StarroadException(ErrorCode.COMMENT_DELETE_FORBIDDEN);
        }
        comment.getBoard().decreaseCommentNum();
        commentRepository.deleteByNo(commentNo);
    }

    private Comment findComment(int commentNo) {
        return commentRepository.findByNo(commentNo)
                .orElseThrow(() -> new StarroadException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
