package com.caspian.chatmemory.config;

import com.caspian.chatmemory.service.ChatAssistant;
import com.caspian.chatmemory.service.ChatPersistenceAssistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * day05 记忆：把模型、记忆窗口和存储装配成声明式助手。
 * <p>
 * 关键点是 {@link ChatMemoryProvider}：它不是返回一个共享的 ChatMemory，
 * 而是按 memoryId 现场创建 {@link MessageWindowChatMemory}，
 * 每个实例都从同一个 {@link ChatMemoryStore} 取回自己的历史，从而实现多会话隔离。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
@Configuration
public class MemoryConfig {

    /**
     * 每个会话保留的消息条数；讲义示例用 10 / 20，这里取 10。
     */
    private static final int MAX_MESSAGES = 10;

    /**
     * 按会话标识产出独立的窗口记忆。
     *
     * @param chatMemoryStore 当前生效的存储实现（内存或 Redis）
     * @return 供 AiServices 使用的记忆提供者
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    /**
     * 装配按 memoryId 隔离记忆的助手。
     *
     * @param chatModel    由 starter 装配的聊天模型
     * @param memoryProvider 记忆提供者
     * @return 多会话助手
     */
    @Bean
    public ChatPersistenceAssistant chatPersistenceAssistant(QwenChatModel chatModel,
                                                             ChatMemoryProvider memoryProvider) {
        return AiServices.builder(ChatPersistenceAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    /**
     * 装配只有一个会话的助手，用于演示窗口记忆下的多轮连续。
     *
     * @param chatModel      由 starter 装配的聊天模型
     * @param memoryProvider 记忆提供者，未标注 {@code @MemoryId} 时使用默认标识
     * @return 单会话助手
     */
    @Bean
    public ChatAssistant chatAssistant(QwenChatModel chatModel,
                                       ChatMemoryProvider memoryProvider) {
        return AiServices.builder(ChatAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

}
