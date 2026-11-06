package com.caspian.chatmemory;

import com.caspian.chatmemory.service.ChatPersistenceAssistant;
import com.caspian.chatmemory.store.InMemoryChatMemoryStore;
import com.caspian.chatmemory.support.FakeChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day05 记忆：多会话隔离测试，是本模块的核心断言。
 *
 * @author Caspian
 * @since 2026-11-06
 */
class MemoryIsolationTest {

    private final FakeChatModel fakeModel = new FakeChatModel();

    private final InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();

    private final ChatPersistenceAssistant assistant = AiServices.builder(ChatPersistenceAssistant.class)
            .chatModel(fakeModel)
            .chatMemoryProvider(provider(store))
            .build();

    @Test
    void sessionsWithDifferentMemoryIdsAreIsolated() {
        assistant.chat(1L, "my name is harry");
        assistant.chat(2L, "my name is cook");

        assistant.chat(1L, "what is my name");

        String sessionOnePrompt = fakeModel.lastRequestText();
        assertTrue(sessionOnePrompt.contains("harry"), "会话 1 应记得自己的历史");
        assertFalse(sessionOnePrompt.contains("cook"), "会话 1 不应看到会话 2 的消息");
    }

    @Test
    void eachSessionStoresItsOwnMessages() {
        assistant.chat(1L, "my name is harry");
        assistant.chat(2L, "my name is cook");

        assertEquals(3, store.getMessages(1L).size(), "系统消息 + 本轮一问一答，共 3 条");
        assertEquals(3, store.getMessages(2L).size());
        assertTrue(store.getMessages(1L).toString().contains("harry"));
        assertFalse(store.getMessages(1L).toString().contains("cook"));
    }

    /**
     * 构造按 memoryId 隔离的 provider。
     *
     * @param chatMemoryStore 共享的底层存储
     * @return 记忆提供者
     */
    private ChatMemoryProvider provider(InMemoryChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

}
