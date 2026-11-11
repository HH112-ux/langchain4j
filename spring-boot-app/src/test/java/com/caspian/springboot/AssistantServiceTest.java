package com.caspian.springboot;

import com.caspian.springboot.service.AssistantService;
import com.caspian.springboot.support.FakeChatModel;
import com.caspian.springboot.support.TestModelsConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day07 Spring Boot 集成：容器启动与声明式服务的离线验证。
 * <p>
 * 用 {@link TestModelsConfig} 顶替 DashScope 的真实模型，
 * 验证 starter 能扫描 {@code @AiService} 生成可注入的代理，
 * 以及 {@code @MemoryId} 在多会话下的隔离效果。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
@SpringBootTest
@Import(TestModelsConfig.class)
class AssistantServiceTest {

    @Autowired
    private AssistantService assistantService;

    @Autowired
    private FakeChatModel chatModel;

    /**
     * 上下文能启动，{@code @AiService} 代理可注入并返回文本，
     * 且请求里带有 {@code @SystemMessage} 与用户问题。
     */
    @Test
    void aiServiceProxyIsInjectableAndReturnsText() {
        assertNotNull(assistantService, "@AiService 代理应作为 bean 注入");

        String reply = assistantService.chat("proxy-check", "LangChain4j 是什么？");

        assertTrue(reply.startsWith(FakeChatModel.REPLY_PREFIX));
        assertTrue(reply.contains("LangChain4j 是什么？"));

        String requestText = flatten(chatModel.requestAt(0));
        assertTrue(requestText.contains("Java 学习助手"), "系统消息应来自 @SystemMessage");
        assertTrue(requestText.contains("LangChain4j 是什么？"));
    }

    /**
     * 同一个 memoryId 的第二轮请求能看到上一轮历史，
     * 另一个 memoryId 的请求则完全看不到，说明记忆按会话隔离。
     */
    @Test
    void memoryIsIsolatedByMemoryId() {
        int baseline = chatModel.requests().size();

        assistantService.chat("session-a", "第一句");
        assistantService.chat("session-a", "第二句");
        assistantService.chat("session-b", "另一个会话");

        assertEquals(baseline + 3, chatModel.requests().size());

        String secondRequest = flatten(chatModel.requestAt(baseline + 1));
        assertTrue(secondRequest.contains("第一句"), "同一会话应带上上一轮用户消息");
        assertTrue(secondRequest.contains("假模型收到：第一句"), "同一会话应带上上一轮模型回复");
        assertTrue(secondRequest.contains("第二句"));

        String thirdRequest = flatten(chatModel.requestAt(baseline + 2));
        assertFalse(thirdRequest.contains("第一句"), "不同会话不应串记忆");
        assertFalse(thirdRequest.contains("第二句"), "不同会话不应串记忆");
        assertTrue(thirdRequest.contains("另一个会话"));
    }

    /**
     * 把一次请求的所有消息拼成文本，便于做包含性断言。
     *
     * @param request 模型收到的请求
     * @return 系统消息、用户消息与模型消息的文本拼接
     */
    private static String flatten(ChatRequest request) {
        return request.messages().stream()
                .map(AssistantServiceTest::textOf)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 提取单条消息的文本。
     *
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
