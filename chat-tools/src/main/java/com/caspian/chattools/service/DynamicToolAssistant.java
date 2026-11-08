package com.caspian.chattools.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

/**
 * day06 工具调用：动态工具助手。
 * <p>
 * 与 {@link ToolAssistant} 的区别是不预先注册任何工具，
 * 而是由 {@code ToolProvider} 在每次请求时根据用户输入决定暴露哪些工具。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
public interface DynamicToolAssistant {

    /**
     * 在指定会话中提问，可用工具由 provider 按请求动态决定。
     *
     * @param userId      会话标识
     * @param userMessage 用户本轮输入
     * @return 带工具执行信息的最终回答
     */
    Result<String> chat(@MemoryId Long userId, @UserMessage String userMessage);

}
