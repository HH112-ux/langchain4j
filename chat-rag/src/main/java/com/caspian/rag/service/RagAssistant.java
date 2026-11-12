package com.caspian.rag.service;

import com.caspian.rag.dto.RagAnswer;
import com.caspian.rag.dto.SearchHit;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * day08 RAG：检索增强问答。
 * <p>
 * 链路是“先检索、后生成”：检索结果由 {@link RetrievalAugmentor} 注入 prompt，
 * 模型只负责总结。检索为空时直接返回固定话术，不再调用模型，
 * 这是抑制“没资料还硬编”的最后一道闸。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
@Service
public class RagAssistant {

    /**
     * 检索不到任何片段时的固定回答，避免模型凭空发挥。
     */
    public static final String NO_KNOWLEDGE_ANSWER = "根据现有知识无法回答。";

    private final KnowledgeAssistant assistant;
    private final ContentRetriever contentRetriever;

    /**
     * 用检索增强器构造声明式助手。
     *
     * @param chatModel        对话模型
     * @param retrievalAugmentor 检索增强器
     * @param contentRetriever  直接检索用，用于无命中预判与调试接口
     */
    public RagAssistant(ChatModel chatModel,
                        RetrievalAugmentor retrievalAugmentor,
                        ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
        this.assistant = AiServices.builder(KnowledgeAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }

    /**
     * 带检索上下文的问答。
     * <p>
     * 这里先检索一次再做判断，随后 {@code AiServices} 调用时增强器会再检索一次，
     * 属于为了“无命中不调用模型”付出的一点重复开销；片段很少时可以接受。
     * </p>
     *
     * @param question 用户问题
     * @return 回答与引用来源
     */
    public RagAnswer chat(String question) {
        List<SearchHit> hits = search(question);
        if (hits.isEmpty()) {
            return new RagAnswer(NO_KNOWLEDGE_ANSWER, List.of(), false);
        }
        Result<String> result = assistant.answer(question);
        List<SearchHit> sources = result.sources().stream()
                .map(SearchHit::from)
                .toList();
        return new RagAnswer(result.content(), sources, true);
    }

    /**
     * 只做检索，不调用模型，方便调试召回效果。
     *
     * @param question 查询文本
     * @return 命中的片段，按相似度或重排分排序
     */
    public List<SearchHit> search(String question) {
        return contentRetriever.retrieve(Query.from(question)).stream()
                .map(SearchHit::from)
                .toList();
    }

    /**
     * 暴露原始检索结果，供需要分数细节的调用方使用。
     *
     * @param question 查询文本
     * @return 框架的检索结果
     */
    public List<Content> retrieve(String question) {
        return contentRetriever.retrieve(Query.from(question));
    }

}
