package com.caspian.springboot.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * day07 Spring Boot 集成：离线测试用的假聊天模型。
 * <p>
 * 在容器里以 {@code qwenChatModel} 为名注册，顶替 starter 生成的 DashScope 模型；
 * 每次调用记录完整请求，用于断言系统消息与按 memoryId 隔离的历史消息。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
public class FakeChatModel implements ChatModel {

    /**
     * 回复前缀，便于断言响应确实来自假模型。
     */
    public static final String REPLY_PREFIX = "假模型收到：";

    private final List<ChatRequest> requests = new CopyOnWriteArrayList<>();

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        requests.add(chatRequest);
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(REPLY_PREFIX + lastUserText(chatRequest)))
                .build();
    }

    /**
     * @return 按调用顺序保存的请求
     */
    public List<ChatRequest> requests() {
        return requests;
    }

    /**
     * @param index 从 0 开始的请求序号
     * @return 对应请求
     */
    public ChatRequest requestAt(int index) {
        return requests.get(index);
    }

    /**
     * 取最后一条用户消息的文本。
     *
     * @param chatRequest 本次请求
     * @return 用户消息文本，没有则返回空串
     */
    private String lastUserText(ChatRequest chatRequest) {
        List<ChatMessage> messages = chatRequest.messages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage userMessage) {
                return userMessage.singleText();
            }
        }
        return "";
    }

}
