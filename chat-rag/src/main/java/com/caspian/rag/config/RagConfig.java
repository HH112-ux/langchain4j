package com.caspian.rag.config;

import com.caspian.rag.rerank.KeywordOverlapScoringModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * day08 RAG：把摄入与检索需要的组件装进容器。
 * <p>
 * 真实模型只在 {@code rag.models=dashscope}（默认）时创建；离线测试通过
 * {@code rag.models=fake} 关掉它们，再由测试配置提供假实现。向量库、检索器、
 * 增强器与模型无关，始终存在。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    /**
     * 真实对话模型，仅用于生成，不参与检索。
     *
     * @param properties 模块配置
     * @return DashScope 对话模型
     */
    @Bean
    @ConditionalOnProperty(name = "rag.models", havingValue = "dashscope", matchIfMissing = true)
    public ChatModel qwenChatModel(RagProperties properties) {
        RagProperties.Dashscope dashscope = properties.dashscope();
        return QwenChatModel.builder()
                .apiKey(dashscope.apiKey())
                .baseUrl(dashscope.baseUrl())
                .modelName(dashscope.chatModel())
                .temperature(0.7f)
                .build();
    }

    /**
     * 真实向量模型，负责摄入与检索时的文本向量化。
     *
     * @param properties 模块配置
     * @return DashScope 向量模型
     */
    @Bean
    @ConditionalOnProperty(name = "rag.models", havingValue = "dashscope", matchIfMissing = true)
    public EmbeddingModel qwenEmbeddingModel(RagProperties properties) {
        RagProperties.Dashscope dashscope = properties.dashscope();
        return QwenEmbeddingModel.builder()
                .apiKey(dashscope.apiKey())
                .baseUrl(dashscope.baseUrl())
                .modelName(dashscope.embeddingModel())
                .dimension(dashscope.embeddingDimension())
                .build();
    }

    /**
     * 进程内向量库，重启即清空；换成真实向量数据库时只替换这个 bean。
     *
     * @return 文本段的向量存储
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 向量检索器：把问题向量化后按余弦相似度取片段。
     * <p>
     * {@code maxResults} 控制上下文长度与噪声，{@code minScore} 控制“宁缺毋滥”，
     * 两者都来自配置文件，避免散落在代码里。
     * </p>
     *
     * @param embeddingStore 向量库
     * @param embeddingModel 向量模型
     * @param properties     模块配置
     * @return 基于向量库的检索器
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                            EmbeddingModel embeddingModel,
                                            RagProperties properties) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(properties.retrieval().maxResults())
                .minScore(properties.retrieval().minScore())
                .build();
    }

    /**
     * 重排打分器。这里用本地关键词重合度代替模型重排，
     * 目的是离线可跑、结果可预期；生产应换成 Jina 或 Cohere 的 rerank 模型。
     *
     * @return 关键词重合度打分器
     */
    @Bean
    public ScoringModel scoringModel() {
        return new KeywordOverlapScoringModel();
    }

    /**
     * 检索增强器：检索之后先重排，再注入 prompt。
     *
     * @param contentRetriever 检索器
     * @param scoringModel     重排打分器
     * @param properties       模块配置
     * @return 默认实现的检索增强器
     */
    @Bean
    public RetrievalAugmentor retrievalAugmentor(ContentRetriever contentRetriever,
                                                 ScoringModel scoringModel,
                                                 RagProperties properties) {
        ReRankingContentAggregator aggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                .maxResults(properties.retrieval().maxResults())
                .build();
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(aggregator)
                .build();
    }

}
