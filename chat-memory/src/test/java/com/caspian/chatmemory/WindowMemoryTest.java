package com.caspian.chatmemory;

import com.caspian.chatmemory.service.ChatAssistant;
import com.caspian.chatmemory.store.InMemoryChatMemoryStore;
import com.caspian.chatmemory.support.FakeChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day05 记忆：窗口记忆的行为测试。
 *
 * @author Caspian
 * @since 2026-11-06
 */
class WindowMemoryTest {

    @Test
    void windowKeepsOnlyLatestMessages() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(4);
        for (int i = 1; i <= 6; i++) {
            memory.add(new UserMessage("u" + i));
        }

        List<ChatMessage> messages = memory.messages();

        assertEquals(4, messages.size(), "窗口上限为 4，超出部分应被裁掉");
        assertEquals("u3", ((UserMessage) messages.get(0)).singleText(), "保留的应是最新的 4 条");
        assertEquals("u6", ((UserMessage) messages.get(3)).singleText());
    }

    @Test
    void secondTurnCarriesFirstTurnHistory() {
        FakeChatModel fakeModel = new FakeChatModel();
        ChatMemoryProvider provider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
        ChatAssistant assistant = AiServices.builder(ChatAssistant.class)
                .chatModel(fakeModel)
                .chatMemoryProvider(provider)
                .build();

        assistant.chat("第一问");
        assistant.chat("第二问");

        int messageCount = fakeModel.lastRequest().messages().size();
        assertEquals(4, messageCount, "系统消息 + 第一轮问答 + 本轮提问");
        assertTrue(fakeModel.lastRequestText().contains("第一问"), "第二轮应带上第一轮的用户消息");
        assertTrue(fakeModel.lastRequestText().contains("第二问"));
    }

}
