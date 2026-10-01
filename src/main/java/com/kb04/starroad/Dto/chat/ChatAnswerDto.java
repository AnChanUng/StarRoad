package com.kb04.starroad.Dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatAnswerDto {

    public static final String OUTCOME_ANSWERED = "answered";
    public static final String OUTCOME_NOT_FOUND = "not_found";
    public static final String OUTCOME_BLOCKED = "blocked";
    public static final String OUTCOME_ERROR = "error";

    private String answer;

    /**
     * 근거 자료를 찾아 답한 경우 true.
     * false 면 검색 단계에서 걸러졌거나 가드레일에 막혀 LLM 답변을 쓰지 않았다는 뜻이다.
     */
    private boolean grounded;

    private List<SourceDto> sources;

    /** 검색은 됐지만 LLM 이 인용번호를 하나도 달지 않은 경우 true (신뢰도 경고용) */
    private boolean uncited;

    /** 가드레일(입력 또는 출력 검사)에 걸려 차단된 경우 true */
    private boolean blocked;

    /** answered | not_found | blocked | error. Actuator 지표의 outcome tag 와 같은 값이다 */
    private String outcome;

    /** 토큰 사용량과 응답 시간 */
    private ChatUsageDto usage;

    /**
     * 근거를 찾지 못했을 때의 응답. LLM 을 호출하지 않는다.
     * (환각 차단 L1 — 검색 게이트)
     */
    public static ChatAnswerDto notFound(String message) {
        return ChatAnswerDto.builder()
                .answer(message)
                .grounded(false)
                .sources(Collections.emptyList())
                .uncited(false)
                .blocked(false)
                .outcome(OUTCOME_NOT_FOUND)
                .usage(ChatUsageDto.none())
                .build();
    }

    /** 가드레일에 걸렸을 때의 응답. 어떤 규칙에 걸렸는지는 사용자에게 알리지 않는다. */
    public static ChatAnswerDto blocked(String message, ChatUsageDto usage) {
        return ChatAnswerDto.builder()
                .answer(message)
                .grounded(false)
                .sources(Collections.emptyList())
                .uncited(false)
                .blocked(true)
                .outcome(OUTCOME_BLOCKED)
                .usage(usage)
                .build();
    }

    public static ChatAnswerDto error(String message) {
        return notFound(message).toBuilder().outcome(OUTCOME_ERROR).build();
    }
}
