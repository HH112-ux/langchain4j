package com.caspian.rag.support;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * day08 RAG：离线测试用的哈希向量模型。
 * <p>
 * 把文本切成单字和双字，按哈希落到固定维度并对向量做归一化。特点：
 * 同一文本每次得到完全相同的向量；共享字词越多，余弦相似度越高。
 * 它不具备真正的语义能力，只用于验证摄入、召回、阈值等流程是否接通。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
public class HashingEmbeddingModel implements EmbeddingModel {

    /**
     * 向量维度。取 256 是为了让测试快且足够区分，与真实模型的 1024 维无关。
     */
    public static final int DIMENSION = 256;

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<Embedding> embeddings = textSegments.stream()
                .map(segment -> Embedding.from(vectorize(segment.text())))
                .toList();
        return Response.from(embeddings);
    }

    /**
     * 把文本转成归一化向量。
     *
     * @param text 原始文本
     * @return 长度为 {@value #DIMENSION} 的向量
     */
    private static float[] vectorize(String text) {
        float[] vector = new float[DIMENSION];
        String normalized = text == null ? "" : text.replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length(); i++) {
            add(vector, normalized.substring(i, i + 1));
            if (i + 1 < normalized.length()) {
                add(vector, normalized.substring(i, i + 2));
            }
        }
        normalize(vector);
        return vector;
    }

    /**
     * 把一个词元累加到哈希位置。
     *
     * @param vector 目标向量
     * @param token  词元
     */
    private static void add(float[] vector, String token) {
        vector[Math.floorMod(token.hashCode(), DIMENSION)] += 1.0f;
    }

    /**
     * 原地做 L2 归一化，保证余弦相似度只与方向有关。
     *
     * @param vector 待归一化向量
     */
    private static void normalize(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0.0) {
            return;
        }
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
    }

}
