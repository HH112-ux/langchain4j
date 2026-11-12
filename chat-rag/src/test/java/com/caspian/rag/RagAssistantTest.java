package com.caspian.rag;

import com.caspian.rag.config.RagProperties;
import com.caspian.rag.dto.RagAnswer;
import com.caspian.rag.rerank.KeywordOverlapScoringModel;
import com.caspian.rag.service.DocumentIngestService;
import com.caspian.rag.service.RagAssistant;
import com.caspian.rag.support.HashingEmbeddingModel;
import com.caspian.rag.support.RecordingChatModel;
import com.caspian.rag.support.TestRagData;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day08 RAG：检索增强问答的离线验证。
 * <p>
 * 两个关键断言：检索到的片段确实出现在发往模型的 prompt 里；检索为空时
 * 直接返回固定话术且不调用模型，避免模型在没有资料时编造。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
class RagAssistantTest {

    /**
     * 有命中时，片段会被增强器拼进 prompt，回答与来源同时返回。
     */
    @Test
    void retrievedContentEntersPrompt() {
        HashingEmbeddingModel embeddingModel = new HashingEmbeddingModel();
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        RagProperties properties = TestRagData.properties(120, 20, 3, 0.0);
        ingest(store, embeddingModel, properties);

        ContentRetriever retriever = retriever(store, embeddingModel, 3, 0.0);
        RecordingChatModel chatModel = new RecordingChatModel();
        RagAssistant assistant = new RagAssistant(chatModel, augmentor(retriever), retriever);

        RagAnswer answer = assistant.chat("差旅报销需要哪些材料");

        assertTrue(answer.matched());
        assertEquals(RecordingChatModel.ANSWER, answer.answer());
        assertFalse(answer.sources().isEmpty(), "应返回检索来源");
        assertTrue(answer.sources().stream().anyMatch(hit -> "报销.md".equals(hit.source())));

        String prompt = chatModel.promptAt(0);
        assertTrue(prompt.contains("报销"), "检索到的片段应注入 prompt");
        assertTrue(prompt.contains("财务总监"), "应带入片段中的具体制度内容");
    }

    /**
     * 没有命中时直接返回“无法回答”，模型一次都不该被调用。
     */
    @Test
    void noHitsSkipsModel() {
        HashingEmbeddingModel embeddingModel = new HashingEmbeddingModel();
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        RagProperties properties = TestRagData.properties(120, 20, 3, 0.99);
        ingest(store, embeddingModel, properties);

        ContentRetriever retriever = retriever(store, embeddingModel, 3, 0.99);
        RecordingChatModel chatModel = new RecordingChatModel();
        RagAssistant assistant = new RagAssistant(chatModel, augmentor(retriever), retriever);

        RagAnswer answer = assistant.chat("量子纠缠火锅怎么做");

        assertFalse(answer.matched());
        assertEquals(RagAssistant.NO_KNOWLEDGE_ANSWER, answer.answer());
        assertTrue(answer.sources().isEmpty());
        assertTrue(chatModel.requests().isEmpty(), "无命中时不应调用模型");
    }

    /**
     * 摄入测试文档。
     *
     * @param store          向量库
     * @param embeddingModel 向量模型
     * @param properties     配置
     */
    private static void ingest(EmbeddingStore<TextSegment> store,
                               HashingEmbeddingModel embeddingModel,
                               RagProperties properties) {
        List<Document> documents = List.of(
                TestRagData.document("报销.md", "差旅报销需要提交出差申请、发票和行程单，超过五千元还要财务总监审批。"),
                TestRagData.document("vpn.md", "远程办公必须连接公司 VPN，账号与邮箱一致。"));
        new DocumentIngestService(embeddingModel, store, properties).ingestManually(documents);
    }

    /**
     * @param store          向量库
     * @param embeddingModel 向量模型
     * @param maxResults     最多条数
     * @param minScore       相似度下限
     * @return 检索器
     */
    private static ContentRetriever retriever(EmbeddingStore<TextSegment> store,
                                              HashingEmbeddingModel embeddingModel,
                                              int maxResults,
                                              double minScore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    /**
     * @param retriever 检索器
     * @return 带重排的检索增强器
     */
    private static RetrievalAugmentor augmentor(ContentRetriever retriever) {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .contentAggregator(ReRankingContentAggregator.builder()
                        .scoringModel(new KeywordOverlapScoringModel())
                        .maxResults(3)
                        .build())
                .build();
    }

}
