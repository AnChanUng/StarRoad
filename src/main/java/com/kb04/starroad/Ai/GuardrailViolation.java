package com.kb04.starroad.Ai;

/**
 * 가드레일에 걸린 사유 1건.
 *
 * @param stage  {@code input}(질문 검사) 또는 {@code output}(답변 검사)
 * @param rule   걸린 규칙 이름. Actuator 지표의 tag 로도 쓰인다
 * @param detail 로그용 상세. 사용자에게는 보여주지 않는다
 */
public record GuardrailViolation(String stage, String rule, String detail) {

    public static final String STAGE_INPUT = "input";
    public static final String STAGE_OUTPUT = "output";

    /** 사용자 화면에 보여줄 문구. 어떤 규칙에 걸렸는지는 드러내지 않는다(우회 힌트가 되므로). */
    public String userMessage() {
        return STAGE_INPUT.equals(stage)
                ? "이 질문은 처리할 수 없습니다. 청년정책이나 예적금 상품에 대해 다시 질문해 주세요."
                : "답변에 제공할 수 없는 내용이 포함되어 표시하지 않았습니다. 질문을 바꿔 다시 시도해 주세요.";
    }
}
