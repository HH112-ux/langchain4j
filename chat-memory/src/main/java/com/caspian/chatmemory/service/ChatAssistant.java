package com.caspian.chatmemory.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * day05 记忆：单会话助手，用窗口记忆演示多轮上下文连续。
 *
 * @author Caspian
 * @since 2026-11-06
 */
public interface ChatAssistant {

    /**
     * 继续当前会话的一轮对话，历史由绑定的 ChatMemory 自动携带。
     *
     * @param userMessage 用户本轮输入
     * @return 模型回复
     */
    @SystemMessage("你是简洁的助手，回答不超过两句话。")
    String chat(@UserMessage String userMessage);

}
