package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.board.BoardResponseDto;
import com.kb04.starroad.Dto.member.DuplicateCheckResponseDto;
import com.kb04.starroad.Dto.member.MemberJoinRequestDto;
import com.kb04.starroad.Dto.member.MemberResponseDto;
import com.kb04.starroad.Dto.mypage.ChallengeResponseDto;
import com.kb04.starroad.Dto.mypage.MemberUpdateRequestDto;
import com.kb04.starroad.Dto.mypage.MyCommentResponseDto;
import com.kb04.starroad.Dto.mypage.MypageResponseDto;
import com.kb04.starroad.Dto.mypage.PasswordCheckResponseDto;
import com.kb04.starroad.Dto.mypage.PasswordRequestDto;
import com.kb04.starroad.Dto.mypage.RewardResponseDto;
import com.kb04.starroad.Entity.*;
import com.kb04.starroad.Exception.ErrorCode;
import com.kb04.starroad.Exception.StarroadException;
import com.kb04.starroad.Repository.*;

import com.kb04.starroad.Repository.Specification.BoardSpecification;
import com.kb04.starroad.Repository.Specification.CommentSpecification;
import com.kb04.starroad.Repository.Specification.PaymentLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MemberService {

    /** 적금 챌린지 리워드 — 가입 기간 1개월당 포인트리 */
    private static final int REWARD_PER_MONTH = 500;

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentLogRepository paymentLogRepository;

    /** 회원가입. 비밀번호는 암호화해서 저장한다. */
    public MemberResponseDto join(MemberJoinRequestDto request) {
        if (!StringUtils.hasText(request.getId()) || !StringUtils.hasText(request.getPassword())) {
            throw new StarroadException(ErrorCode.INVALID_MEMBER_INFO);
        }
        if (memberRepository.findById(request.getId()).isPresent()) {
            throw new StarroadException(ErrorCode.DUPLICATE_MEMBER_ID);
        }
        if (memberRepository.findByEmail(request.getEmail()) != null) {
            throw new StarroadException(ErrorCode.DUPLICATE_MEMBER_EMAIL);
        }

        String encodedPassword = new BCryptPasswordEncoder().encode(request.getPassword());
        Member member = memberRepository.save(request.toEntity(encodedPassword));

        return MemberResponseDto.from(member);
    }

    public DuplicateCheckResponseDto checkId(String id) {
        return DuplicateCheckResponseDto.of(memberRepository.findById(id).isPresent());
    }

    public DuplicateCheckResponseDto checkEmail(String email) {
        return DuplicateCheckResponseDto.of(memberRepository.findByEmail(email) != null);
    }

    public MypageResponseDto getAssets(int no) {
        return MypageResponseDto.of(memberRepository.findByNo(no),
                memberRepository.getSavings(no), memberRepository.getDeposit(no));
    }

    public PasswordCheckResponseDto checkPassword(int no, PasswordRequestDto request) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean passwordMatches = request.getPassword() != null
                && encoder.matches(request.getPassword(), memberRepository.findByNo(no).getPassword());

        return PasswordCheckResponseDto.of(passwordMatches);
    }

    public List<BoardResponseDto> getWritings(int no) {
        return boardRepository.findAll(BoardSpecification.writtenByUser(no)).stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MyCommentResponseDto> getComments(int no) {
        return commentRepository.findAll(CommentSpecification.writtenByUser(no)).stream()
                .map(MyCommentResponseDto::from)
                .collect(Collectors.toList());
    }

    /** 적금 챌린지 — 가입한 상품마다 납입 기록과 진행 상태를 돌려준다 */
    @Transactional
    public List<ChallengeResponseDto> getChallenges(int memberNo) {
        return subscriptionRepository.findByMemberNo(memberNo).stream()
                .map(this::toChallenge)
                .collect(Collectors.toList());
    }

    /**
     * 가입한 상품 하나의 납입 기록을 달별로 정리하고, 지금 상태를 판단한다.
     * 가입 후 n번째 달에 납입했으면 그날의 요일(1~7)을, 안 했으면 0 을 남긴다.
     */
    private ChallengeResponseDto toChallenge(Subscription subscription) {
        int period = subscription.getPeriod();
        Integer[] paymentDays = new Integer[period];
        String[] paymentMonths = new String[period];
        Arrays.fill(paymentDays, 0);

        List<PaymentLog> paylogs = paymentLogRepository.findAll(
                PaymentLogSpecification.getPayLogsBySubNo(subscription.getNo()));
        if (paylogs.isEmpty()) {    // 아직 한 번도 납입하지 않았다
            return ChallengeResponseDto.of(subscription, Arrays.asList(paymentDays), Arrays.asList(paymentMonths), period);
        }

        LocalDate first = toLocalDate(paylogs.get(0));    // 처음 납부 날짜
        LocalDate last = toLocalDate(paylogs.get(paylogs.size() - 1));    // 마지막 납부 날짜
        LocalDate now = LocalDate.now();

        for (PaymentLog paylog : paylogs) {
            LocalDate day = toLocalDate(paylog);
            int month = (int) ChronoUnit.MONTHS.between(first, day);
            if (month < period) {
                paymentMonths[month] = day.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                paymentDays[month] = day.getDayOfWeek().getValue();
            }
        }

        int paidMonths = (int) ChronoUnit.MONTHS.between(first, last);
        boolean success = true;
        for (int m = 0; m < Math.min(paidMonths, period); m++) {
            if (paymentDays[m] == 0) {
                success = false;
                break;
            }
        }

        int status;
        if ((int) ChronoUnit.MONTHS.between(first, now) + 1 >= period) { // 만기?
            // 아직 받지 않았고 꾸준히 넣었는가? 성공 : 실패
            status = (success && subscription.getReceived() == Subscription.REWARD_NOT_RECEIVED)
                    ? ChallengeResponseDto.REWARD_READY : ChallengeResponseDto.FINISHED;
        } else {
            status = period - paidMonths - 1;  // 만기까지 기간이 남았을 때, 남은 개월 수 보내줌.
        }

        return ChallengeResponseDto.of(subscription, Arrays.asList(paymentDays), Arrays.asList(paymentMonths), status);
    }

    private static LocalDate toLocalDate(PaymentLog paylog) {
        return new java.sql.Date(paylog.getPaymentDate().getTime()).toLocalDate();
    }

    /** 완주하면 받을 리워드 조회 */
    @Transactional
    public RewardResponseDto getReward(int memberNo, int subNo) {
        Subscription subscription = findSubscription(memberNo, subNo);
        return RewardResponseDto.of(subscription, rewardOf(subscription));
    }

    /**
     * 리워드 받기. 만기까지 꾸준히 납입했고 아직 받지 않은 가입 건만 받을 수 있다.
     * 포인트리는 서버가 가입 기간으로 계산한다 — 화면에서 보낸 값을 믿지 않는다.
     */
    @Transactional
    public RewardResponseDto receiveReward(int memberNo, int subNo) {
        Subscription subscription = findSubscription(memberNo, subNo);
        if (toChallenge(subscription).getStatus() != ChallengeResponseDto.REWARD_READY) {
            throw new StarroadException(ErrorCode.REWARD_NOT_AVAILABLE);
        }

        int reward = rewardOf(subscription);
        subscription.getMember().savePoint(reward);
        subscription.receiveReward();

        return RewardResponseDto.of(subscription, reward);
    }

    private Subscription findSubscription(int memberNo, int subNo) {
        Subscription subscription = subscriptionRepository.findByNo(subNo);
        if (subscription == null) {
            throw new StarroadException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        if (!subscription.isOwnedBy(memberNo)) {
            throw new StarroadException(ErrorCode.SUBSCRIPTION_FORBIDDEN);
        }
        return subscription;
    }

    private static int rewardOf(Subscription subscription) {
        return subscription.getPeriod() * REWARD_PER_MONTH;
    }

    public MemberResponseDto getInfo(int no) {
        return MemberResponseDto.from(memberRepository.findByNo(no));
    }

    /**
     * 내 정보 수정. 세션에 담긴 회원 정보도 같이 바꿔 화면이 새 값을 보게 한다.
     * @param memberDto 세션의 로그인 회원
     */
    public MemberResponseDto memberUpdate(MemberDto memberDto, MemberUpdateRequestDto changeDto) {
        memberDto.setPhone(changeDto.getPhone());
        memberDto.setEmail(changeDto.getEmail() != null ? changeDto.getEmail() : memberDto.getEmail());
        memberDto.setAddress(changeDto.getAddress());
        memberDto.setJob(changeDto.getJob() != null ? changeDto.getJob() : memberDto.getJob());
        memberDto.setSalary(changeDto.getSalary());
        memberDto.setPurpose(changeDto.getPurpose() != null ? changeDto.getPurpose() : memberDto.getPurpose());
        memberDto.setSource(changeDto.getSource() != null ? changeDto.getSource() : memberDto.getSource());
        memberDto.setGoal(changeDto.getGoal());

        memberRepository.updateMember(memberDto.getNo(), memberDto.getPhone(),
                memberDto.getEmail(), memberDto.getAddress(),
                memberDto.getJob(), memberDto.getSalary(),
                memberDto.getPurpose(), memberDto.getSource(),
                memberDto.getGoal());

        return MemberResponseDto.from(memberDto);
    }

    /** 비밀번호 수정. 암호화해서 저장한다. */
    public void memberPasswordUpdate(MemberDto memberDto, PasswordRequestDto request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new StarroadException(ErrorCode.INVALID_PASSWORD);
        }
        String encPass = new BCryptPasswordEncoder().encode(request.getPassword());
        memberRepository.findByIdAndUpdatePassword(memberDto.getId(), encPass);
        memberDto.setPassword(encPass);
    }
}
