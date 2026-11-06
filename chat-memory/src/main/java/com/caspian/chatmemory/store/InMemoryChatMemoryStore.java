package com.caspian.chatmemory.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * day05 记忆：基于内存 Map 的会话存储，作为 Redis 不可用时的退化实现。
 * <p>
 * 与 {@link RedisChatMemoryStore} 实现同一个 {@link ChatMemoryStore} 接口，
 * 由配置项 {@code chat-memory.store} 在两者之间切换；默认走本实现，
 * 因此没有 Redis 也能启动、编译和跑测试。进程退出即丢数据，仅适合本地练习。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
@Component
@ConditionalOnProperty(name = "chat-memory.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryChatMemoryStore implements ChatMemoryStore {

    private final Map<Object, List<ChatMessage>> messagesByMemoryId = new ConcurrentHashMap<>();

    /**
     * 读取指定会话的消息。
     *
     * @param memoryId 会话标识，对应 {@code @MemoryId} 参数
     * @return 消息列表的副本，会话不存在时返回空列表
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return new ArrayList<>(messagesByMemoryId.getOrDefault(memoryId, List.of()));
    }

    /**
     * 覆盖写入指定会话的消息。
     *
     * @param memoryId 会话标识
     * @param messages 该会话当前完整消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        messagesByMemoryId.put(memoryId, List.copyOf(messages));
    }

    /**
     * 删除指定会话的全部消息。
     *
     * @param memoryId 会话标识
     */
    @Override
    public void deleteMessages(Object memoryId) {
        messagesByMemoryId.remove(memoryId);
    }

}
