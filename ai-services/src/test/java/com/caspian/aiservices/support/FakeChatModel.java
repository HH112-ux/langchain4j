package com.caspian.aiservices.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * day04 AiServices：离线测试用的假聊天模型。
 * <p>
 * 按收到的最后一条用户消息返回固定文本，并保留每次调用收到的 {@link ChatRequest}，
 * 供断言检查系统消息、模板渲染与历史消息组装结果。
 * （1.x 中 {@code ChatLanguageModel} 已更名为 {@link ChatModel}。）
 * </p>
 *
 * @author Caspian
 * @since 2026-11-02
 */
public class FakeChatModel implements ChatModel {

    private final List<ChatRequest> requests = new CopyOnWriteArrayList<>();

    /**
     * @return 按调用顺序保存的所有请求（含系统消息与历史）
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

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        record(chatRequest);
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(replyFor(chatRequest)))
                .build();
    }

    /**
     * 不支持任何高级能力，让 AiServices 走提示词 + JSON 解析的兜底路径。
     *
     * @return 空能力集合
     */
    @Override
    public Set<Capability> supportedCapabilities() {
        return Set.of();
    }

    /**
     * 记录一次请求。
     *
     * @param chatRequest 本次请求
     */
    private void record(ChatRequest chatRequest) {
        requests.add(chatRequest);
    }

    /**
     * 结构化输出用例返回固定 JSON，其余场景回显收到的用户消息。
     *
     * @param chatRequest 本次请求
     * @return 假模型输出文本
     */
    private String replyFor(ChatRequest chatRequest) {
        String prompt = lastUserText(chatRequest);
        if (prompt.contains("课程大纲")) {
            return "{\"title\":\"LangChain4j 入门\",\"chapters\":[\"环境准备\",\"声明式接口\",\"记忆与流式\"]}";
        }
        return "假模型收到：" + prompt;
    }

    /**
     * 取最后一条用户消息的文本。
     *
     * @param chatRequest 本次请求
     * @return 最后一条用户消息文本，没有则返回空串
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
