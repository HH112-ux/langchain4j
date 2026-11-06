package com.caspian.chatmemory.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * day05 记忆：基于 Redis 的会话持久化存储。
 * <p>
 * 把整段消息序列化成 JSON 存进一个 String 键，重启后仍能读回历史；
 * 只有配置 {@code chat-memory.store=redis} 时才会装配，避免没有 Redis 的环境启动失败。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
@Component
@ConditionalOnProperty(name = "chat-memory.store", havingValue = "redis")
public class RedisChatMemoryStore implements ChatMemoryStore {

    /**
     * 键前缀，最终键为 {@code chat:memory:<memoryId>}。
     */
    private static final String KEY_PREFIX = "chat:memory:";

    /**
     * 会话过期时间；讲义未给出，取 7 天的保守默认值。
     */
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    /**
     * @param redisTemplate Spring Boot 依据 spring.data.redis 配置自动装配的字符串模板
     */
    public RedisChatMemoryStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读取指定会话的消息。
     *
     * @param memoryId 会话标识
     * @return 反序列化后的消息列表，键不存在时返回空列表
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(key(memoryId));
        if (json == null) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    /**
     * 覆盖写入指定会话的消息，并刷新过期时间。
     *
     * @param memoryId 会话标识
     * @param messages 该会话当前完整消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        redisTemplate.opsForValue().set(key(memoryId), ChatMessageSerializer.messagesToJson(messages), TTL);
    }

    /**
     * 删除指定会话的消息。
     *
     * @param memoryId 会话标识
     */
    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(key(memoryId));
    }

    /**
     * 拼接 Redis 键名。
     *
     * @param memoryId 会话标识
     * @return 实际写入 Redis 的键
     */
    private String key(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }

}
