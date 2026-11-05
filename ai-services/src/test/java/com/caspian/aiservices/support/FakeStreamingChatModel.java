package com.caspian.aiservices.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * day04 AiServices：离线测试用的假流式模型。
 * <p>
 * 把固定回复拆成三段回调，用于验证 TokenStream 的增量聚合与完成回调；
 * 不访问网络，也不需要 API Key。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-02
 */
public class FakeStreamingChatModel implements StreamingChatModel {

    /**
     * 流式演示用固定回复，测试断言拼接结果与它相等。
     */
    public static final String STREAMING_REPLY = "LangChain4j 是一个 Java 侧的 LLM 编排框架。";

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        handler.onPartialResponse("LangChain4j ");
        handler.onPartialResponse("是一个 Java 侧的 ");
        handler.onPartialResponse("LLM 编排框架。");
        handler.onCompleteResponse(ChatResponse.builder()
                .aiMessage(AiMessage.from(STREAMING_REPLY))
                .build());
    }

}
