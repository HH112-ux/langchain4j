package com.caspian.springboot.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * day07 Spring Boot 集成：离线测试用的假流式模型。
 * <p>
 * 在容器里以 {@code qwenStreamingChatModel} 为名注册，同步回调三段文本后立即完成，
 * 用来验证 {@code Flux<String>} 接口与 SSE 端点的结束行为，不访问网络。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
public class FakeStreamingChatModel implements StreamingChatModel {

    /**
     * 流式回答的完整文本，测试断言 SSE 推送内容等于它。
     */
    public static final String STREAMING_REPLY = "这是流式回答，分三段推送。";

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        handler.onPartialResponse("这是流式回答，");
        handler.onPartialResponse("分三段");
        handler.onPartialResponse("推送。");
        handler.onCompleteResponse(ChatResponse.builder()
                .aiMessage(AiMessage.from(STREAMING_REPLY))
                .build());
    }

}
