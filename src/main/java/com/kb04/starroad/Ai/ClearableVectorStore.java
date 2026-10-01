package com.kb04.starroad.Ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;

/**
 * 통째로 비울 수 있는 SimpleVectorStore.
 *
 * <p>SimpleVectorStore 는 같은 id 를 다시 넣으면 덮어쓸 뿐, 사라진 문서를 지우지 않는다.
 * 청크 크기를 바꾸면 청크 개수가 달라져서(예: 12개 → 8개) 예전 설정으로 만든 조각이
 * 색인에 남는다. 재색인할 때 먼저 비워서 이를 막는다.
 */
public class ClearableVectorStore extends SimpleVectorStore {

    public ClearableVectorStore(EmbeddingModel embeddingModel) {
        super(SimpleVectorStore.builder(embeddingModel));
    }

    public void clear() {
        this.store.clear();
    }

    public int size() {
        return this.store.size();
    }
}
