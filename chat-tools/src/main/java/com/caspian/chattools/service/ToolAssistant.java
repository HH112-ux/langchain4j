package com.caspian.chattools.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * day06 工具调用：高级别助手，工具由 {@code @Tool} 注解声明。
 * <p>
 * 返回 {@link Result} 是为了观察本次执行了哪些工具：
 * {@code content()} 是最终回答，{@code toolExecutions()} 里能看到工具名、参数和回填结果。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
public interface ToolAssistant {

    /**
     * 在指定会话中提问，模型可按需调用已注册的工具。
     *
     * @param userId      会话标识
     * @param userMessage 用户本轮输入
     * @return 带工具执行信息的最终回答
     */
    @SystemMessage("你可以调用天气工具。需要天气或空气质量时先调用工具，再基于工具结果回答，不要编造数据。")
    Result<String> chat(@MemoryId Long userId, @UserMessage String userMessage);

}
