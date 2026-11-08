package com.caspian.chattools.support;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * day06 工具调用：可编程的离线假模型。
 * <p>
 * 工具调用的本质是"模型先返回意图，应用执行后再请求一次模型"，
 * 所以要测这条链路，假模型必须能对第一次和第二次请求给出不同响应。
 * 这里把响应逻辑交给外部函数，同时保留全部请求供断言。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
public class ScriptedChatModel implements ChatModel {

    private final Function<ChatRequest, ChatResponse> responder;

    private final List<ChatRequest> requests = new CopyOnWriteArrayList<>();

    /**
     * @param responder 根据请求生成响应的函数
     */
    public ScriptedChatModel(Function<ChatRequest, ChatResponse> responder) {
        this.responder = responder;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        requests.add(chatRequest);
        return responder.apply(chatRequest);
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
     * @return 最近一次请求
     */
    public ChatRequest lastRequest() {
        return requests.get(requests.size() - 1);
    }

}
