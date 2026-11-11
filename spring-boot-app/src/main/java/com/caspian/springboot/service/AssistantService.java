package com.caspian.springboot.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

/**
 * day07 Spring Boot 集成：声明式 AI 服务。
 * <p>
 * 与 day04 手写 {@code AiServices.builder(Assistant.class).chatModel(...).build()} 不同，
 * 这里只加 {@link AiService}，代理实例由 starter 在启动时扫描生成，可直接注入。
 * </p>
 * <p>
 * {@code wiringMode = EXPLICIT} 表示按名字引用容器里的 bean：{@code qwenChatModel}
 * 与 {@code qwenStreamingChatModel} 由 DashScope starter 依据 {@code application.yaml}
 * 自动创建，{@code chatMemoryProvider} 则是本模块 {@code ChatMemoryConfig} 声明的。
 * 换成 {@code AUTOMATIC} 则要求同一类型在容器里唯一，配置写错时更难看懂，所以这里显式指定。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider")
public interface AssistantService {

    /**
     * 普通问答，一次性返回完整文本。
     *
     * @param conversationId 会话标识，不同值对应不同的记忆窗口
     * @param message        用户问题
     * @return 模型回答
     */
    @SystemMessage("你是 Caspian 的 Java 学习助手。回答简洁，先给结论，再补一两句原因。")
    String chat(@MemoryId String conversationId, @UserMessage String message);

    /**
     * 流式问答，按 token 增量返回。
     * <p>
     * 返回类型为 {@link Flux} 时，starter 会用 {@code streamingChatModel} 建代理，
     * 并由 {@code langchain4j-reactor} 把内部 {@code TokenStream} 适配成响应式流。
     * </p>
     *
     * @param conversationId 会话标识，与 {@link #chat(String, String)} 共用同一份记忆
     * @param message        用户问题
     * @return 逐段返回的回答文本
     */
    @SystemMessage("你是 Caspian 的 Java 学习助手。回答简洁，先给结论，再补一两句原因。")
    Flux<String> chatStream(@MemoryId String conversationId, @UserMessage String message);

}
