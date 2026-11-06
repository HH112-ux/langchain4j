package com.caspian.chatmemory.support;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.TokenCountEstimator;

/**
 * day05 记忆：粗略的 token 估算器，用于 TokenWindowChatMemory 的离线演示。
 * <p>
 * 讲义没有给出估算规则，这里按"每 4 个字符约 1 个 token"的保守默认值实现，
 * 只用于展示 token 窗口的裁剪逻辑，不代表真实模型的计费口径。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
public class CharTokenCountEstimator implements TokenCountEstimator {

    /**
     * 固定按 4 个字符折算 1 个 token。
     */
    private static final int CHARS_PER_TOKEN = 4;

    @Override
    public int estimateTokenCountInText(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage message) {
        return estimateTokenCountInText(message.toString());
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage message : messages) {
            total += estimateTokenCountInMessage(message);
        }
        return total;
    }

}
