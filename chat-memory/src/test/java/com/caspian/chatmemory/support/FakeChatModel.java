package com.caspian.chatmemory.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * day05 记忆：离线测试用的假聊天模型。
 * <p>
 * 记录每次收到的请求并返回固定回复，用于断言系统消息、历史消息与窗口裁剪结果，
 * 全程不联网、不需要 API Key。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
public class FakeChatModel implements ChatModel {

    private final List<ChatRequest> requests = new CopyOnWriteArrayList<>();

    /**
     * @return 按调用顺序保存的所有请求
     */
    public List<ChatRequest> requests() {
        return requests;
    }

    /**
     * @return 最近一次请求
     */
    public ChatRequest lastRequest() {
        return requests.get(requests.size() - 1);
    }

    /**
     * 把最近一次请求里所有消息的文本拼起来，便于做包含性断言。
     *
     * @return 拼接后的文本
     */
    public String lastRequestText() {
        return lastRequest().messages().stream()
                .map(message -> message.toString())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        requests.add(chatRequest);
        return ChatResponse.builder()
                .aiMessage(AiMessage.from("ok"))
                .build();
    }

}
