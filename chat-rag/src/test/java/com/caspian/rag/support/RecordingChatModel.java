package com.caspian.rag.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * day08 RAG：离线测试用的对话模型。
 * <p>
 * 固定回一句可识别的话，并把每次请求完整记录下来；测试据此判断检索到的片段
 * 是否真的进入了 prompt，以及无命中时是否压根没有调用模型。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
public class RecordingChatModel implements ChatModel {

    /**
     * 固定回复，便于断言。
     */
    public static final String ANSWER = "已根据资料回答";

    private final List<ChatRequest> requests = new CopyOnWriteArrayList<>();

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        requests.add(chatRequest);
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(ANSWER))
                .build();
    }

    /**
     * @return 按调用顺序保存的请求
     */
    public List<ChatRequest> requests() {
        return requests;
    }

    /**
     * 取出某次请求的完整 prompt 文本。
     *
     * @param index 从 0 开始的序号
     * @return 系统消息与用户消息的文本拼接
     */
    public String promptAt(int index) {
        return requests.get(index).messages().stream()
                .map(RecordingChatModel::textOf)
                .collect(Collectors.joining("\n"));
    }

    /**
     * @param message 聊天消息
     * @return 文本内容，未知类型返回空串
     */
    private static String textOf(ChatMessage message) {
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        }
        if (message instanceof UserMessage userMessage) {
            return userMessage.singleText();
        }
        if (message instanceof AiMessage aiMessage) {
            return aiMessage.text();
        }
        return "";
    }

}
