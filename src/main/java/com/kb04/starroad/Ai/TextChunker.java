package com.kb04.starroad.Ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 긴 문서를 검색 단위(청크)로 나눈다.
 *
 * <p>정책 공고문은 한 건이 1,000자를 넘는다. 통째로 임베딩하면 여러 주제가 한 벡터에 섞여
 * "제출 서류가 뭐야?" 같은 구체적인 질문과의 유사도가 낮아지고, 임베딩 모델의 입력 한도
 * (multilingual-e5-small 은 512 토큰)를 넘는 뒷부분은 아예 반영되지 않는다.
 *
 * <p>나누는 규칙은 세 가지다.
 * <ul>
 *   <li><b>문장을 자르지 않는다.</b> 문장 단위로 모아 {@code chunkSize} 글자를 넘기 직전에 끊는다.
 *       "월 최대 20만원"이 "월 최대 20" / "만원"으로 갈라지면 숫자가 틀린 근거가 된다.</li>
 *   <li><b>겹침(overlap).</b> 다음 청크는 앞 청크의 마지막 문장들을 {@code overlap} 글자 이내로
 *       다시 포함한다. 조건과 금액이 경계에서 갈라져도 한쪽 청크에는 함께 남는다.</li>
 *   <li><b>소제목은 본문과 붙인다.</b> 소제목으로 끝나는 청크를 만들지 않고, 새 소제목에서
 *       시작하는 청크에는 앞 절의 문장을 겹치지 않는다.</li>
 * </ul>
 *
 * <p>크기 단위는 글자 수다. 토큰 수가 아니라 글자 수로 둔 이유는 설정값을 원문과 눈으로
 * 대조하기 쉬워서다. 한국어는 대략 1글자가 1토큰 안팎이다.
 */
public final class TextChunker {

    /** 문장 끝(마침표·물음표·느낌표) 뒤 공백에서 나눈다. "7,500만원" 의 쉼표나 소수점은 나누지 않는다. */
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");

    private TextChunker() {
    }

    /**
     * @param chunkSize 청크 한 개의 최대 글자 수. 0 이하면 나누지 않고 전체를 한 덩어리로 돌려준다.
     * @param overlap   이웃한 청크가 겹치는 최대 글자 수. {@code chunkSize} 보다 작아야 한다.
     */
    public static List<String> split(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (chunkSize <= 0) {
            return List.of(text.strip());
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap 은 0 이상이고 chunkSize 보다 작아야 한다: chunkSize=" + chunkSize + ", overlap=" + overlap);
        }

        List<String> chunks = new ArrayList<>();
        List<Unit> current = new ArrayList<>();
        // 겹침으로 가져온 문장만 들어 있는 청크는 내보내지 않으려고, 새로 넣은 문장 수를 센다
        int fresh = 0;

        for (Unit unit : units(text, chunkSize, overlap)) {
            if (!current.isEmpty() && length(current) + 1 + unit.text().length() > chunkSize) {
                List<Unit> headings = popTrailingHeadings(current);
                if (!current.isEmpty()) {
                    chunks.add(join(current));
                }
                List<Unit> next = headings.isEmpty() && !unit.heading()
                        ? tail(current, overlap)
                        : new ArrayList<>();
                next.addAll(headings);
                // 겹친 문장 때문에 새 문장이 들어갈 자리가 없으면 겹침을 앞에서부터 줄인다
                while (!next.isEmpty() && length(next) + 1 + unit.text().length() > chunkSize) {
                    next.remove(0);
                }
                current = next;
                fresh = headings.size();
            }
            current.add(unit);
            fresh++;
        }
        if (fresh > 0 && !current.isEmpty()) {
            chunks.add(join(current));
        }
        return chunks;
    }

    /** 문서를 문장·소제목 단위로 쪼갠다. chunkSize 보다 긴 문장은 어쩔 수 없이 글자 수로 자른다. */
    private static List<Unit> units(String text, int chunkSize, int overlap) {
        List<Unit> units = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (isHeading(trimmed) && trimmed.length() <= chunkSize) {
                units.add(new Unit(trimmed, true));
                continue;
            }
            for (String sentence : SENTENCE_END.split(trimmed)) {
                if (sentence.length() <= chunkSize) {
                    units.add(new Unit(sentence, false));
                    continue;
                }
                int step = chunkSize - overlap;
                for (int start = 0; start < sentence.length(); start += step) {
                    units.add(new Unit(sentence.substring(start, Math.min(sentence.length(), start + chunkSize)), false));
                    if (start + chunkSize >= sentence.length()) {
                        break;
                    }
                }
            }
        }
        return units;
    }

    private static boolean isHeading(String line) {
        return line.startsWith("■") || line.startsWith("#");
    }

    private static List<Unit> popTrailingHeadings(List<Unit> units) {
        List<Unit> headings = new ArrayList<>();
        while (!units.isEmpty() && units.get(units.size() - 1).heading()) {
            headings.add(0, units.remove(units.size() - 1));
        }
        return headings;
    }

    /** 끝에서부터 overlap 글자 이내로 들어가는 문장들. 소제목은 겹치지 않는다. */
    private static List<Unit> tail(List<Unit> units, int overlap) {
        List<Unit> tail = new ArrayList<>();
        int total = 0;
        for (int i = units.size() - 1; i >= 0; i--) {
            Unit unit = units.get(i);
            int added = unit.text().length() + (tail.isEmpty() ? 0 : 1);
            if (unit.heading() || total + added > overlap) {
                break;
            }
            tail.add(0, unit);
            total += added;
        }
        return tail;
    }

    private static int length(List<Unit> units) {
        int total = 0;
        for (Unit unit : units) {
            total += unit.text().length();
        }
        return total + Math.max(0, units.size() - 1);
    }

    private static String join(List<Unit> units) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < units.size(); i++) {
            if (i > 0) {
                sb.append(units.get(i).heading() || units.get(i - 1).heading() ? '\n' : ' ');
            }
            sb.append(units.get(i).text());
        }
        return sb.toString();
    }

    private record Unit(String text, boolean heading) {
    }
}
