package com.caspian.chattools;

import com.caspian.chattools.manual.ManualWeatherCaller;
import com.caspian.chattools.support.ScriptedChatModel;
import com.caspian.chattools.support.ToolScripts;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day06 工具调用：低级别手动两轮链路的测试。
 *
 * @author Caspian
 * @since 2026-11-08
 */
class ManualWeatherCallerTest {

    @Test
    void manualLoopSendsToolResultBackToModel() {
        ScriptedChatModel model = new ScriptedChatModel(request -> {
            if (ToolScripts.lastToolResult(request) != null) {
                return ToolScripts.text("预计伦敦明天有小雨。");
            }
            return ToolScripts.toolCall("getWeather", "{\"city\":\"伦敦\"}");
        });
        ManualWeatherCaller caller = new ManualWeatherCaller(model);

        String answer = caller.ask("明天伦敦天气怎么样");

        assertEquals("预计伦敦明天有小雨。", answer);
        assertEquals(2, model.requests().size(), "低级别链路也要请求两次模型");

        ChatRequest second = model.requestAt(1);
        List<ChatMessage> messages = second.messages();
        assertEquals(3, messages.size(), "用户消息 + 第一轮 AI 意图 + 工具结果");
        assertInstanceOf(AiMessage.class, messages.get(1));
        ToolExecutionResultMessage toolResult = assertInstanceOf(ToolExecutionResultMessage.class, messages.get(2));
        assertEquals("getWeather", toolResult.toolName());
        assertTrue(toolResult.text().contains("伦敦"), "应用自己执行工具并回填结果");
        assertFalse(second.toolSpecifications().isEmpty(), "第二轮仍需带上工具规格");
    }

    @Test
    void directAnswerSkipsSecondRound() {
        ScriptedChatModel model = new ScriptedChatModel(request -> ToolScripts.text("你好，我是助手。"));

        String answer = new ManualWeatherCaller(model).ask("你好");

        assertEquals("你好，我是助手。", answer);
        assertEquals(1, model.requests().size(), "模型没请求工具时不需要第二轮");
    }

}
