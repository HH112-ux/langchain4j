package com.caspian.rag.support;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * day08 RAG：上下文测试的假模型装配。
 * <p>
 * 配合 {@code rag.models=fake} 关闭真实 DashScope bean，容器里只保留这两个替身。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
@TestConfiguration
public class FakeModelsConfig {

    /**
     * @return 记录请求的假对话模型
     */
    @Bean
    @Primary
    public RecordingChatModel fakeChatModel() {
        return new RecordingChatModel();
    }

    /**
     * @return 哈希向量模型
     */
    @Bean
    @Primary
    public EmbeddingModel fakeEmbeddingModel() {
        return new HashingEmbeddingModel();
    }

}
