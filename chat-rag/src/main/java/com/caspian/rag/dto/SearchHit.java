package com.caspian.rag.dto;

import com.caspian.rag.service.DocumentIngestService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;

/**
 * day08 RAG：一条检索命中，用于调试检索效果。
 *
 * @param text   命中的片段原文
 * @param score  相似度或重排分，缺失时为 {@code null}
 * @param source 来源文件名
 * @author Caspian
 * @since 2026-11-12
 */
public record SearchHit(String text, Double score, String source) {

    /**
     * 把框架的 {@link Content} 转成对外结构。
     * <p>
     * 纯检索时分数在 {@code SCORE}，经过重排后是 {@code RERANKED_SCORE}，两者取其一。
     * </p>
     *
     * @param content 检索结果
     * @return 可序列化的命中项
     */
    public static SearchHit from(Content content) {
        TextSegment segment = content.textSegment();
        Object score = content.metadata().get(ContentMetadata.SCORE);
        if (score == null) {
            score = content.metadata().get(ContentMetadata.RERANKED_SCORE);
        }
        return new SearchHit(
                segment.text(),
                score instanceof Number number ? number.doubleValue() : null,
                segment.metadata().getString(DocumentIngestService.FILE_NAME_KEY));
    }

}
