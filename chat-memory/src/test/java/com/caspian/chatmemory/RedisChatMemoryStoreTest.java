package com.caspian.chatmemory;

import com.caspian.chatmemory.store.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * day05 记忆：RedisChatMemoryStore 的真实集成测试。
 * <p>
 * 默认连接 {@code localhost:6379}，可用环境变量 {@code REDIS_HOST} / {@code REDIS_PORT} 覆盖。
 * 启动前先探测端口，连不上就 assume-skip，避免没有 Redis 的机器构建失败。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
class RedisChatMemoryStoreTest {

    private static final String HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");

    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    private static final String KEY_PREFIX = "chat:memory:";

    private LettuceConnectionFactory connectionFactory;

    private StringRedisTemplate redisTemplate;

    private RedisChatMemoryStore store;

    private String memoryId;

    @BeforeEach
    void setUp() {
        assumeTrue(redisReachable(), "Redis 不可用（" + HOST + ":" + PORT + "），跳过集成测试");

        connectionFactory = new LettuceConnectionFactory(HOST, PORT);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        store = new RedisChatMemoryStore(redisTemplate);
        memoryId = "it-" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.deleteMessages(memoryId);
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void savedMessagesCanBeReadBack() {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("你是简洁的助手。"),
                UserMessage.from("my name is harry"),
                AiMessage.from("你好，harry。"));

        store.updateMessages(memoryId, messages);

        List<ChatMessage> restored = store.getMessages(memoryId);

        assertEquals(3, restored.size());
        assertInstanceOf(SystemMessage.class, restored.get(0));
        assertEquals("你是简洁的助手。", ((SystemMessage) restored.get(0)).text());
        assertEquals("my name is harry", ((UserMessage) restored.get(1)).singleText());
        assertEquals("你好，harry。", ((AiMessage) restored.get(2)).text());
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + memoryId)),
                "消息应真实写入 Redis 而非只留在内存里");
    }

    @Test
    void updateMessagesReplacesInsteadOfAppending() {
        store.updateMessages(memoryId, List.of(UserMessage.from("first")));
        store.updateMessages(memoryId, List.of(UserMessage.from("second")));

        List<ChatMessage> restored = store.getMessages(memoryId);

        assertEquals(1, restored.size(), "updateMessages 是整体覆盖，不应追加");
        assertEquals("second", ((UserMessage) restored.get(0)).singleText());
    }

    @Test
    void windowEvictionIsWrittenBackToRedis() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(4)
                .chatMemoryStore(store)
                .build();

        for (int i = 1; i <= 6; i++) {
            memory.add(UserMessage.from("u" + i));
        }

        List<ChatMessage> stored = store.getMessages(memoryId);

        assertEquals(4, stored.size(), "写入 Redis 的应是裁剪后的窗口，而不是全部 6 条");
        assertEquals("u3", ((UserMessage) stored.get(0)).singleText());
        assertEquals("u6", ((UserMessage) stored.get(3)).singleText());
    }

    @Test
    void deleteMessagesRemovesStoredHistory() {
        store.updateMessages(memoryId, List.of(UserMessage.from("to be deleted")));
        assertEquals(1, store.getMessages(memoryId).size());

        store.deleteMessages(memoryId);

        assertTrue(store.getMessages(memoryId).isEmpty(), "删除后应读不到该会话的消息");
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + memoryId)),
                "删除后对应的 Redis 键也应消失");
    }

    /**
     * 用短超时探测 Redis 端口，避免未启动时等待 Lettuce 的默认连接超时。
     *
     * @return 端口可连接时返回 true
     */
    private boolean redisReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
