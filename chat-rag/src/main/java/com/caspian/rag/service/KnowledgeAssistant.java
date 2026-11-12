package com.caspian.rag.service;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * day08 RAG：交给 {@code AiServices} 生成的声明式接口。
 * <p>
 * 返回值用 {@link Result} 包一层，是为了在拿到回答的同时拿到检索来源；
 * 如果只关心文本，可以直接返回 {@code String}。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
public interface KnowledgeAssistant {

    /**
     * 基于检索到的资料回答问题。
     *
     * @param question 用户问题，会被检索增强器补上相关资料
     * @return 回答文本与检索来源
     */
    @SystemMessage("""
            你是企业知识库助手。只依据检索到的资料回答，资料里没有就回答“根据现有知识无法回答。”。
            不要使用资料之外的知识，不要编造数字、流程或制度。
            回答简洁，并在末尾用括号注明引用的文件名。
            """)
    Result<String> answer(@UserMessage String question);

}
