package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.policy.PolicyLikeResponseDto;
import com.kb04.starroad.Dto.policy.PolicyNoticeResponseDto;
import com.kb04.starroad.Dto.policy.PolicyPageResponseDto;
import com.kb04.starroad.Dto.policy.PolicyRequestDto;
import com.kb04.starroad.Dto.policy.PolicyResponseDto;
import com.kb04.starroad.Entity.Policy;
import com.kb04.starroad.Entity.PolicyHeart;
import com.kb04.starroad.Exception.ErrorCode;
import com.kb04.starroad.Exception.StarroadException;
import com.kb04.starroad.Repository.MemberRepository;
import com.kb04.starroad.Repository.PolicyHeartRepository;
import com.kb04.starroad.Repository.PolicyRepository;
import com.kb04.starroad.Repository.Specification.PolicySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyHeartRepository policyHeartRepository;
    private final MemberRepository memberRepository;
    private static final int ITEMS_PER_PAGE = 3;
    /** 화면의 '금융자산' 태그는 DB 에 '금융자산 형성' 으로 들어 있다 */
    private static final String TAG4_SUFFIX = " 형성";

    /**
     * 청년정책 조회·검색. 조건이 하나도 없으면 전체를 조회한다.
     * @param loginMember 로그인한 회원. 있으면 관심 정책 여부를 표시한다. 비로그인이면 null
     */
    public PolicyPageResponseDto searchPolicies(PolicyRequestDto request, MemberDto loginMember) {
        Map<String, Object> searchKeys = toSearchKeys(request);

        List<Policy> policies = searchKeys.isEmpty()
                ? policyRepository.findAll()
                : policyRepository.findAll(PolicySpecification.searchPolicyWithMultiConditions(searchKeys));

        Set<Integer> likedPolicyNos = likedPolicyNos(loginMember);
        List<PolicyResponseDto> result = policies.stream()
                .map(policy -> PolicyResponseDto.of(policy, likedPolicyNos.contains(policy.getNo())))
                .collect(Collectors.toList());

        return returnPoliciesByPage(result, request.getPageIndex());
    }

    /**
     * 청년정책 Pagination
     */
    private PolicyPageResponseDto returnPoliciesByPage(List<PolicyResponseDto> policyList, int pageIdx) {
        int totalCount = policyList.size();
        int startIndex = Math.min(Math.max(pageIdx - 1, 0) * ITEMS_PER_PAGE, totalCount);
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalCount);

        return PolicyPageResponseDto.of(policyList.subList(startIndex, endIndex),
                (int) Math.ceil(totalCount / (double) ITEMS_PER_PAGE), pageIdx);
    }

    /**
     * 검색 조건을 PolicySpecification 이 받는 형태로 바꾼다. 비어 있는 조건은 뺀다.
     */
    private Map<String, Object> toSearchKeys(PolicyRequestDto request) {
        Map<String, Object> searchKeys = new HashMap<>();

        if (StringUtils.hasText(request.getLocation())) searchKeys.put("location", request.getLocation());
        if (StringUtils.hasText(request.getKeyword())) searchKeys.put("keyword", request.getKeyword());

        List<String> tags = new ArrayList<>();
        if (StringUtils.hasText(request.getTag1())) tags.add(request.getTag1());
        if (StringUtils.hasText(request.getTag2())) tags.add(request.getTag2());
        if (StringUtils.hasText(request.getTag3())) tags.add(request.getTag3());
        if (StringUtils.hasText(request.getTag4())) tags.add(request.getTag4() + TAG4_SUFFIX);

        if (!tags.isEmpty()) searchKeys.put("tag", tags);

        return searchKeys;
    }

    /**
     * 로그인한 유저가 관심 정책으로 등록한 정책 번호. 비로그인이면 비어 있다.
     */
    private Set<Integer> likedPolicyNos(MemberDto loginMember) {
        if (loginMember == null) {
            return Collections.emptySet();
        }
        return policyHeartRepository.findAllByMemberNo(loginMember.getNo()).stream()
                .map(heart -> heart.getPolicy().getNo())
                .collect(Collectors.toSet());
    }

    /**
     * 관심 정책 등록·해제. 이미 등록한 정책이면 해제하고, 아니면 등록한다.
     * @param memberDto 현재 로그인한 유저
     * @param policyNo 정책 번호
     */
    public PolicyLikeResponseDto togglePolicyHeart(MemberDto memberDto, int policyNo) {
        PolicyHeart policyHeart = policyHeartRepository.findByMemberNoAndPolicyNo(memberDto.getNo(), policyNo);

        if (policyHeart != null) {  // 관심정책에서 삭제
            policyHeartRepository.deleteById(policyHeart.getNo());
            return PolicyLikeResponseDto.of(false);
        }

        // 관심정책으로 등록
        Policy policy = policyRepository.findByNo(policyNo);
        if (policy == null) {
            throw new StarroadException(ErrorCode.POLICY_NOT_FOUND);
        }
        policyHeartRepository.save(PolicyHeart.of(memberRepository.findByNo(memberDto.getNo()), policy));
        return PolicyLikeResponseDto.of(true);
    }

    /**
     * 알림창에 표시할 정책 선별 — 관심 정책 중 아직 마감되지 않았고 마감이 가장 가까운 것
     * @param memberDto 현재 로그인한 유저
     */
    public PolicyNoticeResponseDto modalPolicy(MemberDto memberDto){

        List<Policy> policyList = new ArrayList<>();
        for (PolicyHeart policyHeart : policyHeartRepository.findAllByMemberNo(memberDto.getNo())){
            policyList.add(policyHeart.getPolicy());
        }

        policyList.sort(Comparator.comparing(Policy::getEndDate));

        for (Policy policy : policyList) {
            LocalDate policyDate = policy.getEndDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            long period = ChronoUnit.DAYS.between(policyDate, LocalDate.now());
            if(period <= 0) {
                return PolicyNoticeResponseDto.of(memberDto.getName(), policy, period);
            }
        }
        return PolicyNoticeResponseDto.empty("관심정책을 등록하고 알림을 받아보세요🤗");
    }
}
