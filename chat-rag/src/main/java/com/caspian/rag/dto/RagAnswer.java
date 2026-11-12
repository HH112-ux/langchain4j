package com.caspian.rag.dto;

import java.util.List;

/**
 * day08 RAG：问答响应。
 *
 * @param answer  模型回答
 * @param sources 本次回答实际用到的检索片段
 * @param matched 是否检索到资料；为 {@code false} 时回答是固定的“无法回答”提示
 * @author Caspian
 * @since 2026-11-12
 */
public record RagAnswer(String answer, List<SearchHit> sources, boolean matched) {
}
