package com.caspian.springboot.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * day07 Spring Boot 集成：把记忆交给容器管理。
 * <p>
 * 这里只声明两个 bean，不再像 day04 那样在 {@code AiServices.builder()} 里
 * 直接塞一个 {@code ChatMemory}：
 * </p>
 * <ul>
 *   <li>{@link ChatMemoryStore} 负责真正存消息，默认用进程内实现；</li>
 *   <li>{@link ChatMemoryProvider} 按 memoryId 现场创建窗口记忆，多个会话互不干扰。</li>
 * </ul>
 * <p>
 * 注意必须共享同一个 store：provider 每次请求都会新建一个
 * {@link MessageWindowChatMemory}，若每个实例自带独立的 store，历史在请求结束时就丢了。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 每个会话保留的消息条数，与 day05 的窗口大小保持一致。
     */
    private static final int MAX_MESSAGES = 10;

    /**
     * 进程内消息存储；重启即清空，仅用于学习与演示。
     *
     * @return 共享的聊天记忆存储
     */
    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    /**
     * 按会话标识产出独立的窗口记忆。
     * <p>
     * bean 名称 {@code chatMemoryProvider} 与 {@code @AiService} 的
     * {@code chatMemoryProvider} 属性对应，由 starter 注入到代理里。
     * </p>
     *
     * @param chatMemoryStore 共享的消息存储
     * @return 供 AiService 使用的记忆提供者
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

}
