package com.caspian.chatmemory;

import com.caspian.chatmemory.service.ChatPersistenceAssistant;
import com.caspian.chatmemory.store.InMemoryChatMemoryStore;
import com.caspian.chatmemory.support.FakeChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day05 记忆：存储读回与窗口裁剪落盘行为测试。
 *
 * @author Caspian
 * @since 2026-11-06
 */
class MemoryStoreTest {

    private final InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();

    @Test
    void storeRoundTrip() {
        List<ChatMessage> messages = List.of(new UserMessage("hello"));

        store.updateMessages("s1", messages);
        assertEquals(1, store.getMessages("s1").size());

        store.deleteMessages("s1");
        assertTrue(store.getMessages("s1").isEmpty(), "删除后应读不到消息");
    }

    @Test
    void newMemoryInstanceRestoresHistoryFromStore() {
        FakeChatModel fakeModel = new FakeChatModel();
        ChatPersistenceAssistant assistant = AiServices.builder(ChatPersistenceAssistant.class)
                .chatModel(fakeModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .chatMemoryStore(store)
                        .build())
                .build();
        assistant.chat(7L, "我的名字是 alice");

        // 模拟重启：用同一个 store、同一个 memoryId 重新构造记忆
        MessageWindowChatMemory restored = MessageWindowChatMemory.builder()
                .id(7L)
                .maxMessages(10)
                .chatMemoryStore(store)
                .build();

        assertEquals(3, restored.messages().size(), "应从 store 读回系统消息与一问一答");
        assertTrue(restored.messages().toString().contains("alice"));
    }

    @Test
    void evictionResultIsWrittenBackToStore() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id("s2")
                .maxMessages(4)
                .chatMemoryStore(store)
                .build();
        for (int i = 1; i <= 6; i++) {
            memory.add(new UserMessage("u" + i));
        }

        List<ChatMessage> stored = store.getMessages("s2");

        assertEquals(4, stored.size(), "写入 store 的应是裁剪后的结果，而不是全部 6 条");
        assertEquals("u3", ((UserMessage) stored.get(0)).singleText());
    }

}
