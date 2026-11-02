package com.kb04.starroad;

import com.kb04.starroad.Service.RagIndexService;
import com.kb04.starroad.Repository.PolicyRepository;
import com.kb04.starroad.Repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RagIndexStoreTest {
    @TempDir Path dir;

    private RagIndexService service(VectorStore store, String name) {
        var policies = mock(PolicyRepository.class);
        var products = mock(ProductRepository.class);
        when(policies.findAll()).thenReturn(List.of()); when(products.findAll()).thenReturn(List.of());
        var service = new RagIndexService(store, policies, products);
        ReflectionTestUtils.setField(service, "indexFilePath", dir.resolve(name).toString());
        return service;
    }

    @Test void memoryDeletesStaleManagedDocumentsButPreservesOthersAndRestoresFile() {
        var model = mock(EmbeddingModel.class);
        var store = SimpleVectorStore.builder(model).build();
        // Precomputed embeddings avoid downloads or paid APIs.
        when(model.embed(any(Document.class))).thenAnswer(call -> ((Document)call.getArgument(0)).getId().equals("stale") ? new float[]{1,0} : new float[]{0,1});
        store.add(List.of(new Document("stale", "old policy", Map.of("type", "policy")),
                new Document("other", "unrelated", Map.of("type", "other"))));
        var service = service(store, "memory.json");
        assertThat(service.reindex()).isZero();
        assertThat(dir.resolve("memory.json")).exists();
        var restored = SimpleVectorStore.builder(model).build();
        service(restored, "memory.json").initIndex();
        // A saved index must retain the unrelated record and remove deleted source data.
        when(model.embed("find")).thenReturn(new float[]{0,1});
        assertThat(restored.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder().query("find").topK(10).similarityThresholdAll().build()))
                .extracting(Document::getId).containsExactly("other");
    }

    @Test void externalStoreIgnoresMemoryCacheAndRemovesDeletedSourceRecords() throws Exception {
        var store = mock(VectorStore.class);
        java.nio.file.Files.writeString(dir.resolve("existing.json"), "not a memory index");
        service(store, "existing.json").initIndex();
        verify(store).delete(any(Filter.Expression.class));
        verify(store, never()).add(anyList());
        assertThat(java.nio.file.Files.readString(dir.resolve("existing.json"))).isEqualTo("not a memory index");
    }
}
