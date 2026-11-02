package com.kb04.starroad;

import com.kb04.starroad.Config.VectorStoreConfig;
import com.kb04.starroad.Service.RagIndexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Run against an isolated local Chroma, never a shared production collection. No LLM/API key needed. */
@EnabledIfEnvironmentVariable(named = "CHROMA_TEST_URL", matches = ".+")
class ChromaVectorStoreIntegrationTest {
    @Test void selectsChromaPersistsAcrossConnectionsAndSupportsManagedDeletion() {
        var model = mock(EmbeddingModel.class);
        when(model.dimensions()).thenReturn(2);
        when(model.embed(anyList(), any(), any())).thenAnswer(call -> ((List<Document>)call.getArgument(0)).stream()
                .map(doc -> doc.getText().contains("policy") ? new float[]{1,0} : new float[]{0,1}).toList());
        when(model.embed(anyString())).thenReturn(new float[]{1,0});
        String collection = "starroad-test-" + UUID.randomUUID();
        var runner = new ApplicationContextRunner().withUserConfiguration(VectorStoreConfig.class)
                .withBean(EmbeddingModel.class, () -> model)
                .withPropertyValues("starroad.rag.store=chroma", "starroad.rag.chroma.url=" + System.getenv("CHROMA_TEST_URL"),
                        "starroad.rag.chroma.collection=" + collection);
        var query = SearchRequest.builder().query("policy").topK(4).similarityThreshold(0.85).build();
        String id = UUID.randomUUID().toString();
        runner.run(context -> {
            assertThat(context).hasSingleBean(VectorStore.class);
            var store = context.getBean(VectorStore.class);
            assertThat(store.getClass().getSimpleName()).isEqualTo("ChromaVectorStore");
            store.add(List.of(new Document(id, "policy", Map.of("type", "policy")),
                    new Document(UUID.randomUUID().toString(), "unrelated", Map.of("type", "other"))));
            assertThat(store.similaritySearch(query)).extracting(Document::getId).containsExactly(id);
        });
        runner.run(context -> {
            var store = context.getBean(VectorStore.class);
            assertThat(store.similaritySearch(query)).extracting(Document::getId).containsExactly(id);
            var filter = new FilterExpressionBuilder();
            store.delete(filter.or(filter.eq(RagIndexService.META_TYPE, "policy"), filter.eq(RagIndexService.META_TYPE, "product")).build());
            assertThat(store.similaritySearch(query)).isEmpty();
            // Remove only this test's remaining documents.
            store.delete(filter.eq("type", "other").build());
        });
    }
}
