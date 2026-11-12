package com.caspian.rag;

import com.caspian.rag.config.RagProperties;
import com.caspian.rag.support.HashingEmbeddingModel;
import com.caspian.rag.support.TestRagData;
import com.caspian.rag.service.DocumentIngestService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day08 RAG：向量检索的离线验证。
 * <p>
 * 验证三件事：相关文档排在前面、{@code maxResults} 限制条数、{@code minScore}
 * 过滤低分片段。哈希向量只能表达字面重合，因此查询文本都使用文档里的关键词。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
class RetrievalTest {

    private EmbeddingStore<TextSegment> store;

    private HashingEmbeddingModel embeddingModel;

    private RagProperties properties;

    /**
     * 每个用例使用独立的向量库，避免用例间互相影响。
     */
    @BeforeEach
    void setUp() {
        properties = TestRagData.properties(120, 20, 3, 0.0);
        embeddingModel = new HashingEmbeddingModel();
        store = new InMemoryEmbeddingStore<>();

        List<Document> documents = List.of(
                TestRagData.document("报销.md", "差旅报销需要提交出差申请、发票和行程单，超过五千元还要财务总监审批。"),
                TestRagData.document("vpn.md", "远程办公必须连接公司 VPN，账号与邮箱一致，连续三次失败会被锁定。"),
                TestRagData.document("年假.md", "入职满一年有五天带薪年假，可以顺延到次年三月三十一日。"));
        new DocumentIngestService(embeddingModel, store, properties).ingestManually(documents);
    }

    /**
     * 查询“报销”时，讲报销的片段应排在第一位。
     */
    @Test
    void retrievesMostRelevantSegmentFirst() {
        List<Content> hits = retriever(3, 0.0).retrieve(Query.from("差旅报销需要哪些材料"));

        assertFalse(hits.isEmpty());
        assertTrue(hits.get(0).textSegment().text().contains("报销"), "最相关的片段应排第一");
    }

    /**
     * maxResults 限制返回条数。
     */
    @Test
    void maxResultsLimitsReturnedSegments() {
        assertEquals(1, retriever(1, 0.0).retrieve(Query.from("报销 VPN 年假")).size());

        List<Content> all = retriever(10, 0.0).retrieve(Query.from("报销 VPN 年假"));
        assertTrue(all.size() > 1, "提高 maxResults 后应能拿到更多片段");
    }

    /**
     * minScore 越高越严格：阈值为 0 时有结果，接近 1 时全部被过滤。
     */
    @Test
    void minScoreFiltersWeakMatches() {
        assertFalse(retriever(3, 0.0).retrieve(Query.from("报销")).isEmpty());
        assertTrue(retriever(3, 0.99).retrieve(Query.from("报销")).isEmpty());
    }

    /**
     * 构造指定参数的检索器。
     *
     * @param maxResults 最多条数
     * @param minScore   相似度下限
     * @return 检索器
     */
    private ContentRetriever retriever(int maxResults, double minScore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

}
