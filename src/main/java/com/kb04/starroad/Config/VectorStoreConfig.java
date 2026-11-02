package com.kb04.starroad.Config;

import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class VectorStoreConfig {
    @Bean
    @ConditionalOnProperty(name = "starroad.rag.store", havingValue = "memory", matchIfMissing = true)
    public SimpleVectorStore memoryVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    @ConditionalOnProperty(name = "starroad.rag.store", havingValue = "chroma")
    public ChromaVectorStore chromaVectorStore(EmbeddingModel embeddingModel,
            @Value("${starroad.rag.chroma.url:http://localhost:8000}") String url,
            @Value("${starroad.rag.chroma.collection:starroad}") String collection,
            @Value("${starroad.rag.chroma.tenant:default_tenant}") String tenant,
            @Value("${starroad.rag.chroma.database:default_database}") String database) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        var api = ChromaApi.builder().baseUrl(url)
                .restClientBuilder(RestClient.builder().requestFactory(factory)).build();
        return ChromaVectorStore.builder(api, embeddingModel)
                .tenantName(tenant).databaseName(database).collectionName(collection)
                .initializeSchema(true).build();
    }
}
