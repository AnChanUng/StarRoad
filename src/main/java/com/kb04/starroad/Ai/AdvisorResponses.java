package com.kb04.starroad.Ai;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Advisor 가 모델을 부르지 않고 직접 응답을 만들 때 쓰는 도우미. */
final class AdvisorResponses {

    private AdvisorResponses() {
    }

    /** 모델 호출 없이 고정 문구로 응답한다. 토큰 사용량은 0 으로 남는다. */
    static ChatClientResponse shortCircuit(String text, Map<String, Object> context,
                                           String key, Object value) {
        Map<String, Object> merged = new HashMap<>(context);
        merged.put(key, value);
        return ChatClientResponse.builder()
                .chatResponse(ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(text))))
                        .build())
                .context(merged)
                .build();
    }

    /**
     * 모델 응답 본문만 바꿔 치운다. 메타데이터(토큰 사용량)는 그대로 둔다.
     * 차단된 답변도 토큰은 이미 쓴 것이므로 비용 집계에서 빠지면 안 된다.
     */
    static ChatClientResponse replaceText(ChatClientResponse response, String text,
                                          String key, Object value) {
        ChatResponse original = response.chatResponse();
        ChatResponseMetadata metadata = original == null ? new ChatResponseMetadata() : original.getMetadata();
        Map<String, Object> merged = new HashMap<>(response.context());
        merged.put(key, value);
        return ChatClientResponse.builder()
                .chatResponse(ChatResponse.builder()
                        .metadata(metadata)
                        .generations(List.of(new Generation(new AssistantMessage(text))))
                        .build())
                .context(merged)
                .build();
    }

    static String text(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null
                || response.chatResponse().getResult() == null
                || response.chatResponse().getResult().getOutput() == null) {
            return null;
        }
        return response.chatResponse().getResult().getOutput().getText();
    }
}
