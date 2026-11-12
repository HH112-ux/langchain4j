package com.caspian.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * day08 RAG：本模块的可调参数。
 * <p>
 * 摄入与检索的数值不是随手写的：{@code chunkSize} 决定单段信息量，
 * {@code chunkOverlap} 决定跨段语义不被切断；{@code maxResults} 与
 * {@code minScore} 决定最终有多少内容进入 prompt。注释里写清了取值理由。
 * </p>
 *
 * @param models     模型来源，{@code dashscope} 用真实模型，测试里切到 {@code fake}
 * @param dashscope  DashScope 连接参数
 * @param ingest     摄入参数
 * @param retrieval  检索参数
 * @author Caspian
 * @since 2026-11-12
 */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(String models, Dashscope dashscope, Ingest ingest, Retrieval retrieval) {

    /**
     * DashScope 相关配置。
     *
     * @param apiKey      API Key，未配置时为空串，真实调用会失败但应用能启动
     * @param baseUrl     DashScope 原生接口地址
     * @param chatModel   对话模型名
     * @param embeddingModel 向量模型名
     * @param embeddingDimension 向量维度
     */
    public record Dashscope(String apiKey, String baseUrl, String chatModel,
                            String embeddingModel, Integer embeddingDimension) {
    }

    /**
     * 摄入参数。
     *
     * @param chunkSize   单段最大字符数
     * @param chunkOverlap 相邻段重叠字符数
     * @param docsPattern 文档位置，支持 classpath 通配
     */
    public record Ingest(int chunkSize, int chunkOverlap, String docsPattern) {
    }

    /**
     * 检索参数。
     *
     * @param maxResults 单次最多返回的片段数
     * @param minScore   相似度下限，低于该值的片段视为不相关
     */
    public record Retrieval(int maxResults, double minScore) {
    }

}
