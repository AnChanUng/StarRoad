package com.kb04.starroad;

import com.kb04.starroad.Config.VectorStoreConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VectorStoreSelectionTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(VectorStoreConfig.class)
            .withBean(EmbeddingModel.class, () -> mock(EmbeddingModel.class));

    @Test void defaultUsesMemoryWithoutContactingChroma() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(VectorStore.class);
            assertThat(context.getBean(VectorStore.class)).isInstanceOf(SimpleVectorStore.class);
        });
    }
    @Test void explicitMemoryIgnoresAnUnreachableChromaAddress() {
        runner.withPropertyValues("starroad.rag.store=memory", "starroad.rag.chroma.url=http://127.0.0.1:1")
                .run(context -> assertThat(context).hasSingleBean(SimpleVectorStore.class));
    }
}
