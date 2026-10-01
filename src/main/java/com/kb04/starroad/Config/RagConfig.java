package com.kb04.starroad.Config;

import com.kb04.starroad.Ai.GuardrailAdvisor;
import com.kb04.starroad.Ai.RagRetrievalAdvisor;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    /**
     * 인메모리 벡터 저장소.
     *
     * <p>정책·상품은 수백 건 규모라 인메모리로 충분하다. 다만 Spring AI 문서상
     * SimpleVectorStore 는 데모·테스트 용도로 명시돼 있다 — 데이터가 수만 건으로
     * 늘거나 다중 인스턴스로 배포하게 되면 pgvector·Redis 등으로 교체해야 한다.
     * VectorStore 인터페이스를 그대로 쓰므로 교체 시 이 빈 정의만 바뀐다.
     */
    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 챗봇용 ChatClient. 모든 호출이 아래 Advisor 체인을 거친다.
     * <ol>
     *   <li>{@link GuardrailAdvisor} — 입력 검사 / (돌아올 때) 출력 검사</li>
     *   <li>{@link RagRetrievalAdvisor} — L1 검색 게이트 + L2 자료 주입</li>
     *   <li>LLM 호출</li>
     * </ol>
     * 실행 순서는 등록 순서가 아니라 각 Advisor 의 {@code getOrder()} 로 정해진다.
     *
     * <p>자동설정된 {@code ChatClient.Builder} 를 쓰는 이유는 ObservationRegistry 가 연결돼
     * 있어서다. 그래야 토큰 사용량·호출 시간이 Actuator 지표로 기록된다.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 GuardrailAdvisor guardrailAdvisor,
                                 RagRetrievalAdvisor ragRetrievalAdvisor) {
        return builder.defaultAdvisors(guardrailAdvisor, ragRetrievalAdvisor).build();
    }

    /**
     * Spring AI 1.1.8 은 Anthropic 요청에 temperature 를 기본값 0.8 로 항상 실어 보낸다.
     * 그런데 Claude Opus 5 계열은 temperature·top_p·top_k 가 제거된 모델이라 이 값이
     * 붙으면 400 을 돌려준다.
     *
     * <p>요청 옵션은 {@code merge(런타임, 기본값)} 으로 병합되므로 런타임에서 null 을 줘도
     * 기본값이 되살아난다. 따라서 기본 옵션 객체 자체를 비워야 한다.
     * ChatCompletionRequest 가 {@code @JsonInclude(NON_NULL)} 이라, null 이면 요청 본문에서 빠진다.
     *
     * <p>temperature 를 쓰는 구형 모델(claude-sonnet-4-5 등)로 되돌린다면 이 빈을 제거하면 된다.
     */
    @Bean
    static BeanPostProcessor anthropicSamplingOptionsRemover() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof AnthropicChatProperties properties) {
                    AnthropicChatOptions options = properties.getOptions();
                    options.setTemperature(null);
                    options.setTopP(null);
                    options.setTopK(null);
                }
                return bean;
            }
        };
    }
}
