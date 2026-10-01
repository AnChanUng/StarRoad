package com.kb04.starroad;

import com.kb04.starroad.Ai.TextChunker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 청크 분할 규칙. 임베딩 모델 없이 돈다. */
class TextChunkerTest {

    private static final String NOTICE = """
            ■ 지원 대상
            만 19세 이상 34세 이하 무주택 청년이 대상이다. 임차보증금 5천만원 이하이고 월세 70만원 이하인 주택에 살아야 한다.

            ■ 소득 기준
            청년 가구 소득이 기준 중위소득 60% 이하여야 한다. 원가구 소득은 기준 중위소득 100% 이하여야 한다. 재산은 1억 2,200만원 이하일 때 인정된다.

            ■ 제출 서류
            임대차계약서 사본과 최근 3개월간 월세를 이체한 증빙 서류를 제출한다. 가족관계증명서와 통장 사본도 함께 낸다.
            """;

    @Test
    void chunkSizeZero_returnsWholeText() {
        List<String> chunks = TextChunker.split(NOTICE, 0, 0);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("■ 지원 대상", "통장 사본도 함께 낸다.");
    }

    @Test
    void blankText_returnsNoChunks() {
        assertThat(TextChunker.split("  \n ", 100, 10)).isEmpty();
        assertThat(TextChunker.split(null, 100, 10)).isEmpty();
    }

    @Test
    void chunksNeverExceedChunkSize() {
        for (int size : new int[]{60, 100, 150, 300}) {
            for (String chunk : TextChunker.split(NOTICE, size, 20)) {
                assertThat(chunk.length()).as("chunkSize=%d 인데 청크가 더 길다: %s", size, chunk).isLessThanOrEqualTo(size);
            }
        }
    }

    @Test
    void sentencesAreNotCut() {
        List<String> chunks = TextChunker.split(NOTICE, 100, 0);

        // 숫자가 든 문장이 통째로 한 청크 안에 있어야 한다
        assertThat(chunks).anyMatch(c -> c.contains("재산은 1억 2,200만원 이하일 때 인정된다."));
        assertThat(chunks).anyMatch(c -> c.contains("임차보증금 5천만원 이하이고 월세 70만원 이하인 주택에 살아야 한다."));
        // 쉼표가 든 금액에서 끊지 않는다
        assertThat(chunks).noneMatch(c -> c.endsWith("1억 2,") || c.startsWith("200만원"));
    }

    @Test
    void everySentenceSurvives() {
        List<String> chunks = TextChunker.split(NOTICE, 90, 30);
        String joined = String.join("\n", chunks);

        for (String line : NOTICE.split("\\R")) {
            for (String sentence : line.strip().split("(?<=[.])\\s+")) {
                if (!sentence.isBlank()) {
                    assertThat(joined).contains(sentence);
                }
            }
        }
    }

    @Test
    void overlapRepeatsLastSentenceOfPreviousChunk() {
        String text = "첫째 문장은 지원 대상을 설명한다. 둘째 문장은 소득 기준을 설명한다. "
                + "셋째 문장은 제출 서류를 설명한다. 넷째 문장은 신청 방법을 설명한다.";

        List<String> without = TextChunker.split(text, 45, 0);
        List<String> with = TextChunker.split(text, 45, 25);

        assertThat(without).containsExactly(
                "첫째 문장은 지원 대상을 설명한다. 둘째 문장은 소득 기준을 설명한다.",
                "셋째 문장은 제출 서류를 설명한다. 넷째 문장은 신청 방법을 설명한다.");
        assertThat(with).containsExactly(
                "첫째 문장은 지원 대상을 설명한다. 둘째 문장은 소득 기준을 설명한다.",
                "둘째 문장은 소득 기준을 설명한다. 셋째 문장은 제출 서류를 설명한다.",
                "셋째 문장은 제출 서류를 설명한다. 넷째 문장은 신청 방법을 설명한다.");
    }

    @Test
    void headingStaysWithItsBody() {
        List<String> chunks = TextChunker.split(NOTICE, 100, 30);

        // 소제목만 덩그러니 끝에 달린 청크가 없어야 한다
        assertThat(chunks).noneMatch(c -> c.strip().endsWith("■ 소득 기준") || c.strip().endsWith("■ 제출 서류"));
        // 새 절은 소제목으로 시작하고, 앞 절의 문장을 겹쳐 오지 않는다
        assertThat(chunks).anyMatch(c -> c.startsWith("■ 소득 기준\n청년 가구 소득이"));
        assertThat(chunks).anyMatch(c -> c.startsWith("■ 제출 서류\n임대차계약서 사본과"));
    }

    @Test
    void sentenceLongerThanChunkSize_isCutByLength() {
        String longSentence = "가".repeat(250);

        List<String> chunks = TextChunker.split(longSentence, 100, 20);

        assertThat(chunks).allMatch(c -> c.length() <= 100);
        assertThat(String.join("", chunks)).contains("가".repeat(100));
        assertThat(chunks.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void overlapMustBeSmallerThanChunkSize() {
        assertThatThrownBy(() -> TextChunker.split(NOTICE, 100, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextChunker.split(NOTICE, 100, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
