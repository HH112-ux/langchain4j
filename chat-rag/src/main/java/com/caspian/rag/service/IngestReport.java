package com.caspian.rag.service;

/**
 * day08 RAG：一次摄入的结果摘要。
 *
 * @param documentCount 原始文档数
 * @param segmentCount  切分后的片段数，也就是进入向量库的条数
 * @author Caspian
 * @since 2026-11-12
 */
public record IngestReport(int documentCount, int segmentCount) {
}
