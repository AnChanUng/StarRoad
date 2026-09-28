package com.kb04.starroad;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 스프링 컨텍스트가 정상적으로 뜨는지 확인한다.
 *
 * <p>dev 프로필로 도는 이유: 기본 설정은 Oracle(localhost:1521)을 바라보기 때문에
 * DB 가 떠 있지 않은 환경에서는 컨텍스트 로딩이 실패한다. dev 프로필의 인메모리 H2 를
 * 쓰면 외부 의존 없이 검증할 수 있다.
 */
@SpringBootTest(properties = "spring.ai.vectorstore.chroma.collection-name=starroad-test-klue-v1")
@ActiveProfiles("dev")
class StarroadApplicationTests {

    @Test
    void contextLoads() {
    }

}
