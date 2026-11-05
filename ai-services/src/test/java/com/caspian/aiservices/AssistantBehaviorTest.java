package com.caspian.aiservices;

import com.caspian.aiservices.assistant.Assistant;
import com.caspian.aiservices.assistant.CourseOutline;
import com.caspian.aiservices.support.FakeChatModel;
import com.caspian.aiservices.support.FakeStreamingChatModel;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day04 AiServices：离线行为测试。
 * <p>
 * 全部用例使用 {@link FakeChatModel}，不发起网络请求、不需要 API Key。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-02
 */
class AssistantBehaviorTest {

    private final FakeChatModel fakeModel = new FakeChatModel();

    private final FakeStreamingChatModel fakeStreamingModel = new FakeStreamingChatModel();

    private final Assistant assistant = AiServices.builder(Assistant.class)
            .chatModel(fakeModel)
            .streamingChatModel(fakeStreamingModel)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();

    @Test
    void declarativeCallReturnsModelText() {
        String reply = assistant.chat("你好");

        assertEquals("假模型收到：你好", reply);
    }

    @Test
    void systemMessageIsInjected() {
        assistant.chat("你好");

        ChatRequest request = fakeModel.lastRequest();
        SystemMessage systemMessage = assertInstanceOf(SystemMessage.class, request.messages().get(0));

        assertEquals("你是一位严谨的 Java 讲师，回答控制在三句话以内。", systemMessage.text());
    }

    @Test
    void userMessageVariableIsRendered() {
        assistant.explain("ChatMemory");

        UserMessage userMessage = assertInstanceOf(UserMessage.class, lastUserMessage(fakeModel.lastRequest()));
        String text = userMessage.singleText();

        assertTrue(text.contains("ChatMemory"), "模板里的占位符应被方法参数替换");
        assertTrue(text.contains("用两句话说明"));
    }

    @Test
    void memoryCarriesPreviousTurns() {
        assistant.chat("第一问");
        assistant.chat("第二问");

        // 系统消息 + 第一轮 user/ai + 本轮 user
        assertEquals(4, fakeModel.lastRequest().messages().size());
        assertTrue(lastUserMessage(fakeModel.lastRequest()).singleText().contains("第二问"));
    }

    @Test
    void structuredOutputIsParsed() {
        CourseOutline outline = assistant.outline("LangChain4j");

        assertEquals("LangChain4j 入门", outline.title());
        assertEquals(List.of("环境准备", "声明式接口", "记忆与流式"), outline.chapters());
    }

    @Test
    void streamingEmitsIncrementalChunks() throws InterruptedException {
        List<String> partials = new CopyOnWriteArrayList<>();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        assistant.stream("LangChain4j")
                .onPartialResponse(partials::add)
                .onCompleteResponse(response -> completed.countDown())
                .onError(throwable -> {
                    error.set(throwable);
                    completed.countDown();
                })
                .start();

        assertTrue(completed.await(5, TimeUnit.SECONDS), "流式回调应在超时前结束");
        assertNull(error.get());
        assertEquals(FakeStreamingChatModel.STREAMING_REPLY, String.join("", partials));
        assertTrue(partials.size() > 1, "应收到多个增量片段");
    }

    /**
     * 取请求中最后一条用户消息。
     *
     * @param request 模型收到的请求
     * @return 最后一条用户消息
     */
    private UserMessage lastUserMessage(ChatRequest request) {
        for (int i = request.messages().size() - 1; i >= 0; i--) {
            if (request.messages().get(i) instanceof UserMessage userMessage) {
                return userMessage;
            }
        }
        throw new AssertionError("请求中没有用户消息");
    }

}
