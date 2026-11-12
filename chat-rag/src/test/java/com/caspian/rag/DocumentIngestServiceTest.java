package com.caspian.rag;

import com.caspian.rag.config.RagProperties;
import com.caspian.rag.dto.SearchHit;
import com.caspian.rag.service.DocumentIngestService;
import com.caspian.rag.service.IngestReport;
import com.caspian.rag.support.HashingEmbeddingModel;
import com.caspian.rag.support.TestRagData;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day08 RAG：摄入链路的离线验证。
 * <p>
 * 对应课程里的“加载 → 切分 → 向量化 → 入库”，重点验证切分边界与两条入库路径
 * 产出一致的结果，全程使用哈希向量模型，不访问网络。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
class DocumentIngestServiceTest {

    /**
     * 三段独立的短段落，用来验证“按段落切分”的默认行为。
     */
    private static final String THREE_PARAGRAPHS = """
            第一段讲差旅报销：出差前提交申请，结束后十五个工作日内报销。
            第二段讲年假制度：入职满一年有五天带薪年假，可以顺延到次年三月。
            第三段讲远程办公：每周三可以申请，期间必须连接公司 VPN。
            """;

    /**
     * 一段连续长文本，用来验证段长上限与重叠。
     */
    private static final String LONG_PARAGRAPH = "公司知识库用于保存制度与流程。".repeat(18);

    /**
     * 段长小于单段文本时按行切开，三段各成一段。
     * <p>
     * 注意：递归分片器会尽量把相邻短段合并到接近上限，所以这里把段长调到 40，
     * 否则三段会被合并成一段——这一点在默认的 300 字符下同样成立。
     * </p>
     */
    @Test
    void splitsParagraphsIntoOneSegmentEach() {
        DocumentIngestService service = service(TestRagData.properties(40, 0, 3, 0.0));

        List<TextSegment> segments = service.split(List.of(TestRagData.document("policy.md", THREE_PARAGRAPHS)));

        assertEquals(3, segments.size());
        assertTrue(segments.get(0).text().contains("差旅报销"));
        assertTrue(segments.get(1).text().contains("年假"));
        assertTrue(segments.get(2).text().contains("远程办公"));
        segments.forEach(segment -> assertTrue(segment.text().length() <= 40));
    }

    /**
     * 超长段落按字符切分：段数符合预期，每段不超过上限，且后一段带上前一段的尾部。
     */
    @Test
    void capsSegmentLengthAndKeepsOverlap() {
        DocumentIngestService service = service(TestRagData.properties(100, 20, 3, 0.0));

        List<TextSegment> segments = service.split(List.of(TestRagData.document("long.md", LONG_PARAGRAPH)));

        assertEquals(3, segments.size());
        assertTrue(segments.stream().allMatch(segment -> segment.text().length() <= 100));

        String previousText = segments.get(0).text();
        String nextText = segments.get(1).text();
        int overlap = 0;
        for (int length = Math.min(previousText.length(), nextText.length()); length > 0; length--) {
            if (previousText.endsWith(nextText.substring(0, length))) {
                overlap = length;
                break;
            }
        }
        // 递归分片器会把重叠分摊到不同层级，配置 20 时实测有效重叠为 10。
        assertTrue(overlap >= 10, "相邻段应保留重叠，实际重叠 " + overlap);
    }

    /**
     * 手动分步与 Ingestor 的段数一致，入库后可以检索到对应片段。
     */
    @Test
    void manualPipelineMatchesIngestorAndIsSearchable() {
        RagProperties properties = TestRagData.properties(100, 20, 3, 0.0);
        HashingEmbeddingModel embeddingModel = new HashingEmbeddingModel();
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        DocumentIngestService service = new DocumentIngestService(embeddingModel, store, properties);

        List<Document> documents = List.of(
                TestRagData.document("报销.md", "差旅报销需要提交申请并上传发票。"),
                TestRagData.document("vpn.md", "远程办公必须连接公司 VPN 才能访问内部系统。"));

        IngestReport report = service.ingestManually(documents);

        assertEquals(2, report.documentCount());
        assertEquals(2, report.segmentCount());

        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.0)
                .build();
        List<SearchHit> hits = retriever.retrieve(Query.from("报销需要什么材料")).stream()
                .map(SearchHit::from)
                .toList();

        assertTrue(hits.stream().anyMatch(hit -> hit.text().contains("报销")));
        assertTrue(hits.stream().anyMatch(hit -> "报销.md".equals(hit.source())));
    }

    /**
     * 构造使用独立向量库的服务。
     *
     * @param properties 配置
     * @return 摄入服务
     */
    private static DocumentIngestService service(RagProperties properties) {
        return new DocumentIngestService(new HashingEmbeddingModel(), new InMemoryEmbeddingStore<>(), properties);
    }

}
