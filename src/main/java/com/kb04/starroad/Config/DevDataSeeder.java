package com.kb04.starroad.Config;

import com.kb04.starroad.Entity.BaseRate;
import com.kb04.starroad.Entity.Board;
import com.kb04.starroad.Entity.Member;
import com.kb04.starroad.Entity.PaymentLog;
import com.kb04.starroad.Entity.Policy;
import com.kb04.starroad.Entity.Product;
import com.kb04.starroad.Entity.Subscription;
import com.kb04.starroad.Repository.BaseRateRepository;
import com.kb04.starroad.Repository.BoardRepository;
import com.kb04.starroad.Repository.MemberRepository;
import com.kb04.starroad.Repository.PaymentLogRepository;
import com.kb04.starroad.Repository.PolicyRepository;
import com.kb04.starroad.Repository.ProductRepository;
import com.kb04.starroad.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * dev 프로필 전용 샘플 데이터.
 *
 * <p>인메모리 DB 는 기동할 때마다 비어 있으므로 정책·상품을 다시 넣는다.
 * 로그인해야 볼 수 있는 화면(마이페이지, 만기 예상 금액)을 확인할 수 있게 체험용 회원도 한 명 넣는다.
 * {@code CommandLineRunner} 는 {@code ApplicationReadyEvent} 보다 먼저 실행되므로,
 * {@link com.kb04.starroad.Service.RagIndexService} 가 색인을 만들 시점에는
 * 이 데이터가 이미 DB 에 들어가 있다.
 *
 * <p>운영에서는 이 클래스가 아예 로딩되지 않는다.
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    /** 체험용 회원 계정. dev 프로필에서만 만들어진다. */
    static final String DEMO_ID = "starroad1";
    static final String DEMO_PASSWORD = "starroad1234";

    private final PolicyRepository policyRepository;
    private final ProductRepository productRepository;
    private final BaseRateRepository baseRateRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final BoardRepository boardRepository;

    @Override
    public void run(String... args) {
        if (policyRepository.count() > 0) {
            return;
        }

        policyRepository.saveAll(List.of(
                policy("청년 월세 한시 특별지원", "서울", "생활지원",
                        "만 19~34세 무주택 청년에게 월 최대 20만원의 월세를 최장 12개월간 지원한다. "
                                + "본인 소득이 중위소득 60% 이하이고 원가구 소득이 중위소득 100% 이하여야 신청할 수 있다. "
                                + "보증금 5천만원 이하, 월세 70만원 이하 주택 거주자가 대상이다.",
                        "https://www.gov.kr/portal/rcvfvrSvc/dtlEx/policy-monthly-rent", 90),

                policy("청년내일저축계좌", "중앙부처", "금융자산 형성",
                        "일하는 저소득 청년이 매월 10만원을 저축하면 정부가 10만원에서 30만원을 함께 적립해 준다. "
                                + "3년 만기 시 본인 저축액에 정부 지원금을 더한 목돈을 받는다. "
                                + "만 19~34세, 근로소득이 월 50만원 초과 230만원 이하인 청년이 대상이다.",
                        "https://www.bokjiro.go.kr/youth-savings", 45),

                policy("청년도약계좌", "중앙부처", "금융자산 형성",
                        "만 19~34세 청년이 매월 최대 70만원까지 5년간 납입하면 정부 기여금과 비과세 혜택을 더해 "
                                + "약 5천만원의 목돈을 만들 수 있는 정책형 적금이다. "
                                + "개인소득 7,500만원 이하, 가구소득 중위 250% 이하 조건을 충족해야 한다.",
                        "https://www.kinfa.or.kr/youth-leap-account", 200),

                policy("서울시 청년수당", "서울", "생활지원",
                        "서울에 거주하는 미취업 청년에게 월 50만원씩 최대 6개월간 활동지원금을 지급한다. "
                                + "만 19~34세, 최종학교 졸업 후 2년이 지난 미취업자가 신청할 수 있으며 "
                                + "구직활동 계획서를 제출해야 한다.",
                        "https://youth.seoul.go.kr/youth-allowance", 30),

                policy("청년 전세보증금 반환보증 보증료 지원", "경기", "생활지원",
                        "전세보증금 반환보증에 가입한 청년에게 납부한 보증료 전액을 최대 30만원까지 돌려준다. "
                                + "만 19~39세, 연소득 5천만원 이하, 전세보증금 3억원 이하 임차인이 대상이다.",
                        "https://www.gg.go.kr/jeonse-guarantee-support", 120),

                policy("국민취업지원제도 청년특례", "중앙부처", "교육",
                        "취업을 준비하는 청년에게 월 50만원의 구직촉진수당을 최대 6개월간 지급하고 "
                                + "직업훈련과 취업알선을 함께 제공한다. "
                                + "만 15~34세이며 가구소득이 중위소득 120% 이하인 경우 신청할 수 있다.",
                        "https://www.work.go.kr/kua", 60)
        ));

        List<Product> products = productRepository.saveAll(List.of(
                // type: 'S' = 적금, 'D' = 예금
                product('S', "KB청년희망적금", "청년우대",
                        "만 19~34세 청년을 위한 우대금리 적금이다. 급여이체와 자동이체를 등록하면 "
                                + "우대금리가 더해진다. 매월 최대 50만원까지 자유롭게 납입할 수 있다.",
                        12, 36, 10000, 500000, 5.00, 24, 1.20,
                        "https://obank.kbstar.com/product/youth-hope-savings"),

                product('S', "KB내집마련 청년적금", "주택청약",
                        "내 집 마련을 준비하는 청년을 위한 적금으로, 만기 시 주택자금 대출 금리를 우대해 준다. "
                                + "청약통장을 함께 보유하면 추가 우대금리가 적용된다.",
                        24, 60, 50000, 1000000, 4.50, 36, 0.80,
                        "https://obank.kbstar.com/product/home-ready-savings"),

                product('D', "KB Star 정기예금", "일반",
                        "목돈을 안정적으로 굴리고 싶을 때 쓰는 기본 정기예금이다. "
                                + "가입 기간을 길게 잡을수록 금리가 높아지며 만기 자동재예치를 신청할 수 있다.",
                        6, 36, 1000000, null, 3.80, null, null,
                        "https://obank.kbstar.com/product/star-time-deposit"),

                product('D', "KB 첫재테크 예금", "청년우대",
                        "사회초년생을 위한 소액 정기예금이다. 첫 거래 고객과 급여이체 고객에게 우대금리를 준다. "
                                + "100만원부터 가입할 수 있어 부담이 적다.",
                        6, 24, 1000000, 30000000, 4.10, 12, 0.50,
                        "https://obank.kbstar.com/product/first-finance-deposit")
        ));

        seedDemoMember(products.get(0), products.get(1));

        log.info("[dev] 샘플 데이터 적재 완료 — 정책 {}건, 상품 {}건, 체험용 계정 {} / {}",
                policyRepository.count(), productRepository.count(), DEMO_ID, DEMO_PASSWORD);
    }

    /**
     * 체험용 회원과 그 회원의 적금 가입·납입 기록, 게시글.
     *
     * @param finished 12개월을 모두 납입해 리워드를 받을 수 있는 적금
     * @param ongoing  3개월째 납입 중인 적금
     */
    private void seedDemoMember(Product finished, Product ongoing) {
        Member member = memberRepository.save(Member.join("김별길", DEMO_ID,
                new BCryptPasswordEncoder().encode(DEMO_PASSWORD), "1999-03-01", "010-1234-5678",
                "starroad1@example.com", "서울 영등포구 국제금융로8길 26,101동 1001호",
                "직장인", "저축 및 투자", "근로 및 연금소득", 3000, 30));

        // 가입 기간별 기본 금리 — 상품 검색에서 기간을 고르면 이 금리로 만기 예상 금액을 계산한다
        baseRateRepository.saveAll(List.of(
                BaseRate.of(finished, 12, 23, 3.20),
                BaseRate.of(finished, 24, 36, 3.80),
                BaseRate.of(ongoing, 24, 35, 3.30),
                BaseRate.of(ongoing, 36, 60, 3.70)));

        Subscription finishedSub = subscriptionRepository.save(Subscription.subscribe(member, finished, 12, 200));
        for (int month = 12; month >= 1; month--) {
            paymentLogRepository.save(PaymentLog.of(finishedSub, monthsAgo(month)));
        }
        Subscription ongoingSub = subscriptionRepository.save(Subscription.subscribe(member, ongoing, 24, 100));
        for (int month = 2; month >= 0; month--) {
            paymentLogRepository.save(PaymentLog.of(ongoingSub, monthsAgo(month)));
        }

        boardRepository.saveAll(List.of(
                Board.write(member, "F", "정보공유", "청년도약계좌 가입 후기",
                        "매달 70만원씩 넣고 있는데 정부 기여금이 생각보다 쏠쏠하네요.", null),
                Board.write(member, "C", "챌린지", "적금 12개월 완주했습니다",
                        "한 번도 안 빼먹고 넣었어요. 다음은 24개월 도전합니다!", null)));
    }

    private Policy policy(String name, String location, String tag, String explain,
                          String link, int endInDays) {
        return Policy.of(name, location, tag, explain, link, daysFromNow(endInDays));
    }

    private Product product(char type, String name, String attribute, String explain,
                            int minPeriod, int maxPeriod, int minPrice, Integer maxPrice,
                            double maxRate, Integer maxRatePeriod, Double maxConditionRate,
                            String link) {
        return Product.of(type, name, attribute, explain, minPeriod, maxPeriod, minPrice, maxPrice,
                maxRate, maxRatePeriod, maxConditionRate, link);
    }

    private Date monthsAgo(int months) {
        return Date.from(LocalDate.now().minusMonths(months)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date daysFromNow(int days) {
        return Date.from(LocalDate.now().plusDays(days)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
