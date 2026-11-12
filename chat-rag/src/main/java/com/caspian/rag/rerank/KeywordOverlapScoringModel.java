package com.caspian.rag.rerank;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;

import java.util.List;

/**
 * day08 RAG 高级检索：用本地关键词重合度替代模型重排。
 * <p>
 * 课程里的 ReRankingContentAggregator 通常接 Jina/Cohere 的交叉编码器。
 * 为了让整个模块离线可跑，这里用字符二元组的 Jaccard 相似度打分：对同一批候选片段
 * 复用同一个打分器，结果稳定、可解释，也足以演示“先召回、再精排”的位置。
 * 生产环境应把本类替换成真正的 rerank 模型。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
public class KeywordOverlapScoringModel implements ScoringModel {

    /**
     * 给每段候选文本打分。
     *
     * @param segments 候选片段，顺序与返回值一致
     * @param query    用户原始问题
     * @return 每段的 0~1 分，分越高越相关
     */
    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        List<Double> scores = segments.stream()
                .map(segment -> jaccard(bigrams(segment.text()), bigrams(query)))
                .toList();
        return Response.from(scores);
    }

    /**
     * 计算两个二元组集合的 Jaccard 相似度。
     *
     * @param left  候选文本的二元组
     * @param right 问题的二元组
     * @return 交集大小除以并集大小，空集返回 0
     */
    private static double jaccard(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        long intersection = left.stream().filter(right::contains).count();
        long union = left.size() + right.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    /**
     * 把文本拆成相邻字符的二元组。
     *
     * @param text 原始文本
     * @return 二元组列表，文本过短时返回空列表
     */
    private static List<String> bigrams(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", "");
        if (normalized.length() < 2) {
            return List.of();
        }
        return java.util.stream.IntStream.range(0, normalized.length() - 1)
                .mapToObj(i -> normalized.substring(i, i + 2))
                .toList();
    }

}
