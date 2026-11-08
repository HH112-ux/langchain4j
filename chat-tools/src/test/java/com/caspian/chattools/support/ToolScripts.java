package com.caspian.chattools.support;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * day06 工具调用：测试里构造假模型响应的工具方法。
 *
 * @author Caspian
 * @since 2026-11-08
 */
public final class ToolScripts {

    private ToolScripts() {
    }

    /**
     * 构造一次"模型请求调用工具"的响应。
     *
     * @param toolName  工具名
     * @param arguments 工具参数 JSON
     * @return 带工具调用请求的响应
     */
    public static ChatResponse toolCall(String toolName, String arguments) {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1")
                .name(toolName)
                .arguments(arguments)
                .build();
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(request))
                .build();
    }

    /**
     * 构造一次普通文本响应。
     *
     * @param text 回复文本
     * @return 响应对象
     */
    public static ChatResponse text(String text) {
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(text))
                .build();
    }

    /**
     * 取请求中最后一条工具结果消息。
     *
     * @param request 聊天请求
     * @return 最后一条工具结果消息，没有则返回 {@code null}
     */
    public static ToolExecutionResultMessage lastToolResult(ChatRequest request) {
        for (int i = request.messages().size() - 1; i >= 0; i--) {
            ChatMessage message = request.messages().get(i);
            if (message instanceof ToolExecutionResultMessage toolResult) {
                return toolResult;
            }
        }
        return null;
    }

}
