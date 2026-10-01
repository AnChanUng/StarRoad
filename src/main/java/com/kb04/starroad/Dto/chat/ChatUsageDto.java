package com.kb04.starroad.Dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 요청 1건의 비용과 속도. 평가 리포트·포트폴리오에 수정 전후를 비교하는 근거가 된다.
 *
 * <p>LLM 을 부르지 않은 요청(L1 차단, 입력 가드레일 차단)은 토큰이 0 이다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUsageDto {

    private int promptTokens;

    private int completionTokens;

    private int totalTokens;

    /** 질문을 받은 시점부터 응답을 만들 때까지 걸린 시간 */
    private long elapsedMs;

    public static ChatUsageDto none() {
        return new ChatUsageDto(0, 0, 0, 0L);
    }
}
