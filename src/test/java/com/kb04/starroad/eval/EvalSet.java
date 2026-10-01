package com.kb04.starroad.eval;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** src/test/resources/eval/rag-eval-set.json 을 읽는다. */
public final class EvalSet {

    public static final String ANSWERABLE = "answerable";
    public static final String NOT_IN_DOCS = "not_in_docs";
    public static final String OUT_OF_DOMAIN = "out_of_domain";
    public static final String ATTACK = "attack";

    /** 평가 결과 파일이 쌓이는 곳. target 아래라 커밋되지 않는다. */
    public static final Path REPORT_DIR = Path.of("target", "rag-eval");

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public record Case(String id, String category, String question,
                       String expectedSource, List<String> mustContain) {

        public List<String> mustContainOrEmpty() {
            return mustContain == null ? List.of() : mustContain;
        }
    }

    public record File(String description, List<Case> cases) {
    }

    private EvalSet() {
    }

    public static List<Case> load() {
        try (InputStream in = EvalSet.class.getResourceAsStream("/eval/rag-eval-set.json")) {
            if (in == null) {
                throw new IllegalStateException("eval/rag-eval-set.json 이 없다");
            }
            return MAPPER.readValue(in, File.class).cases();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Case> byCategory(List<Case> cases, String category) {
        return cases.stream().filter(c -> category.equals(c.category())).toList();
    }

    public static void write(String fileName, String content) {
        try {
            Files.createDirectories(REPORT_DIR);
            Files.writeString(REPORT_DIR.resolve(fileName), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeJson(String fileName, Object value) {
        try {
            write(fileName, MAPPER.writeValueAsString(value));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** src/test/resources/eval 아래 기준선 파일. 없으면 null. */
    public static <T> T readBaseline(String fileName, Class<T> type) {
        try (InputStream in = EvalSet.class.getResourceAsStream("/eval/" + fileName)) {
            return in == null ? null : MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
