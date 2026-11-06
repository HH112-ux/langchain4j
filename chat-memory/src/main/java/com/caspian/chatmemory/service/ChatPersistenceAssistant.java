package com.caspian.chatmemory.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * day05 记忆：多会话助手，用 {@code @MemoryId} 按会话隔离记忆。
 * <p>
 * 与单会话助手的区别只在多了一个被 {@code @MemoryId} 标注的参数：
 * 框架用它作为 key 向 {@code ChatMemoryProvider} 取对应的 {@code ChatMemory}，
 * 不同 id 的历史互不可见。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
public interface ChatPersistenceAssistant {

    /**
     * 在指定会话中继续对话。
     *
     * @param memoryId    会话标识，同一标识共享历史
     * @param userMessage 用户本轮输入
     * @return 模型回复
     */
    @SystemMessage("你是简洁的助手，回答不超过两句话。")
    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);

}
