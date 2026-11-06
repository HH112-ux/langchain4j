package com.caspian.chatmemory;

import com.caspian.chatmemory.support.CharTokenCountEstimator;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day05 记忆：MessageWindow 与 TokenWindow 两种裁剪策略的差异演示。
 *
 * @author Caspian
 * @since 2026-11-06
 */
class TokenWindowComparisonTest {

    private final TokenCountEstimator estimator = new CharTokenCountEstimator();

    @Test
    void tokenWindowEvictsByEstimatedTokens() {
        TokenWindowChatMemory memory = TokenWindowChatMemory.builder()
                .maxTokens(20, estimator)
                .build();
        for (int i = 1; i <= 6; i++) {
            memory.add(new UserMessage("这是一条比较长的用户消息，编号 " + i));
        }

        int tokens = estimator.estimateTokenCountInMessages(memory.messages());

        assertTrue(tokens <= 20, "裁剪后总 token 不应超过上限，实际 " + tokens);
        assertTrue(memory.messages().size() < 6, "应裁掉部分消息");
    }

    @Test
    void messageWindowCountsMessagesNotTokens() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(6);
        for (int i = 1; i <= 6; i++) {
            memory.add(new UserMessage("这是一条比较长的用户消息，编号 " + i));
        }

        assertEquals(6, memory.messages().size(), "按条数保留，不看内容长短");
    }

}
