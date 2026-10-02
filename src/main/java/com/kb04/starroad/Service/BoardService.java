package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.board.BoardMainResponseDto;
import com.kb04.starroad.Dto.board.BoardResponseDto;
import com.kb04.starroad.Dto.board.BoardSummaryDto;
import com.kb04.starroad.Dto.board.BoardUpdateRequestDto;
import com.kb04.starroad.Dto.board.BoardWriteRequestDto;
import com.kb04.starroad.Dto.board.LikeResponseDto;
import com.kb04.starroad.Entity.Board;
import com.kb04.starroad.Entity.Heart;
import com.kb04.starroad.Entity.Member;
import com.kb04.starroad.Exception.ErrorCode;
import com.kb04.starroad.Exception.StarroadException;
import com.kb04.starroad.Repository.BoardRepository;
import com.kb04.starroad.Repository.HeartRepository;
import com.kb04.starroad.Repository.MemberRepository;
import com.kb04.starroad.Repository.Specification.BoardSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BoardService {

    private static final String TYPE_FREE = "F";
    private static final String TYPE_AUTH = "C";
    /** 게시판 메인에 게시판마다 보여 줄 글 수 */
    private static final int MAIN_LIST_SIZE = 6;
    /** 인기글 기준 — 최근 7일 안에 좋아요를 이만큼 받은 글 */
    private static final int POPULAR_MIN_LIKES = 10;

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final HeartRepository heartRepository;
    private final CommentService commentService;

    /**
     * 게시판 메인 — 인기글, 자유게시판, 인증방의 최신 글
     */
    public BoardMainResponseDto getMain() {
        Pageable pageable = PageRequest.of(0, MAIN_LIST_SIZE, Sort.by("regdate").descending());

        return BoardMainResponseDto.of(
                toSummaries(boardRepository.findAllByStatusAndLikesGreaterThanEqualAndRegdateAfterOrderByLikesDesc(
                        'Y', POPULAR_MIN_LIKES, oneWeekAgo(), pageable)),
                toSummaries(boardRepository.findAllByTypeAndStatusOrderByRegdateDesc(TYPE_FREE, 'Y', pageable)),
                toSummaries(boardRepository.findAllByTypeAndStatusOrderByRegdateDesc(TYPE_AUTH, 'Y', pageable)));
    }

    /**
     * 게시판 모든 글 출력 - 자유 게시판, 인증방
     * @param type 게시판 종류 (F: 자유게시판, C: 인증방)
     */
    public List<BoardResponseDto> selectBoardAllOrderByDate(String type) {
        if (!TYPE_FREE.equals(type) && !TYPE_AUTH.equals(type)) {
            throw new StarroadException(ErrorCode.INVALID_BOARD_TYPE);
        }
        return boardRepository.findAll(BoardSpecification.searchBoardByStatusAndType(type, 'Y')).stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }

    // 게시판 모든 글 출력 - 인기글
    public List<BoardResponseDto> selectPopularBoard() {
        return boardRepository.findAllByStatusAndLikesGreaterThanEqualAndRegdateAfterOrderByLikesDesc(
                        'Y', POPULAR_MIN_LIKES, oneWeekAgo()).stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 게시물 상세 보기 — 댓글 포함
     * @param no 게시물 번호
     */
    public BoardResponseDto detailBoard(int no) {
        Board board = findBoard(no);
        return BoardResponseDto.of(board, commentService.findByBoard(board));
    }

    /**
     * 게시물 등록
     * @param memberId 로그인한 회원 아이디
     */
    @Transactional
    public BoardResponseDto writeBoard(String memberId, BoardWriteRequestDto request) {
        Member writer = findMember(memberId);

        Board board = boardRepository.save(Board.write(writer, request.getType(), request.getDetailType(),
                request.getTitle(), request.getContent(), toBytes(request.getImage())));

        return BoardResponseDto.from(board);
    }

    /**
     * 게시물 수정. 작성자 본인만 할 수 있다. 새 이미지를 보내지 않으면 기존 이미지를 유지한다.
     * @param no 게시물 번호
     * @param memberId 로그인한 회원 아이디
     */
    @Transactional
    public BoardResponseDto updateBoard(int no, String memberId, BoardUpdateRequestDto request) {
        Board board = findBoard(no);
        if (!board.isWrittenBy(memberId)) {
            throw new StarroadException(ErrorCode.BOARD_UPDATE_FORBIDDEN);
        }

        byte[] newImage = toBytes(request.getNewImage());
        board.update(request.getTitle(), request.getContent(), newImage == null ? board.getImage() : newImage);

        return BoardResponseDto.from(board);
    }

    /**
     * 게시물 삭제. 작성자 본인만 할 수 있다.
     * @param no 게시물 번호
     * @param memberId 로그인한 회원 아이디
     */
    @Transactional
    public void deleteBoard(int no, String memberId) {
        Board board = findBoard(no);
        if (!board.isWrittenBy(memberId)) {
            throw new StarroadException(ErrorCode.BOARD_DELETE_FORBIDDEN);
        }
        boardRepository.delete(board);
    }

    /**
     * 게시물 좋아요 누르기. 한 회원이 같은 글에 한 번만 누를 수 있다.
     * @param boardNo 게시물 번호
     * @param memberId 로그인한 유저 아이디
     */
    @Transactional
    public LikeResponseDto increaseLikes(int boardNo, String memberId) {
        Board board = findBoard(boardNo);
        Member member = findMember(memberId);

        if (heartRepository.findByMemberNoAndBoardNo(member.getNo(), boardNo).isPresent()) {
            throw new StarroadException(ErrorCode.BOARD_ALREADY_LIKED);
        }
        board.increaseLikes();
        heartRepository.save(Heart.of(member, board));

        return LikeResponseDto.of(board.getLikes());
    }

    private Board findBoard(int no) {
        Board board = boardRepository.findByNo(no);
        if (board == null) {
            throw new StarroadException(ErrorCode.BOARD_NOT_FOUND);
        }
        return board;
    }

    private Member findMember(String memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new StarroadException(ErrorCode.LOGIN_REQUIRED));
    }

    private static List<BoardSummaryDto> toSummaries(Page<Board> boards) {
        return boards.getContent().stream()
                .map(BoardSummaryDto::from)
                .collect(Collectors.toList());
    }

    private static Date oneWeekAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        return calendar.getTime();
    }

    /** 첨부 파일을 바이트로 읽는다. 파일을 보내지 않았으면 null */
    private static byte[] toBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new StarroadException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }
}
