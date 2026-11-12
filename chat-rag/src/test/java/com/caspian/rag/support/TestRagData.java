package com.caspian.rag.support;

import com.caspian.rag.config.RagProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;

/**
 * day08 RAG：测试用的配置与文档构造器。
 * <p>
 * 把重复的 {@link RagProperties} 构造集中起来，测试只关心本次要验证的数值。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
public final class TestRagData {

    private TestRagData() {
    }

    /**
     * 构造一份测试配置。
     *
     * @param chunkSize  段长
     * @param overlap    重叠
     * @param maxResults 最大命中数
     * @param minScore   相似度下限
     * @return 配置对象
     */
    public static RagProperties properties(int chunkSize, int overlap, int maxResults, double minScore) {
        return new RagProperties(
                "fake",
                new RagProperties.Dashscope("", "http://localhost", "qwen-plus", "text-embedding-v4", 1024),
                new RagProperties.Ingest(chunkSize, overlap, "classpath:docs/*.md"),
                new RagProperties.Retrieval(maxResults, minScore));
    }

    /**
     * 构造一篇带来源文件名的文档。
     *
     * @param fileName 文件名
     * @param text     正文
     * @return 文档
     */
    public static Document document(String fileName, String text) {
        Metadata metadata = new Metadata();
        metadata.put("file_name", fileName);
        return Document.from(text, metadata);
    }

}
