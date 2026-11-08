package com.caspian.chattools;

import com.caspian.chattools.service.ToolAssistant;
import com.caspian.chattools.support.ScriptedChatModel;
import com.caspian.chattools.support.ToolScripts;
import com.caspian.chattools.tool.WeatherTools;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day06 工具调用：工具调用的两类兜底——幻觉工具名与无限循环。
 *
 * @author Caspian
 * @since 2026-11-08
 */
class ToolSafetyTest {

    private final WeatherTools weatherTools = new WeatherTools();

    @Test
    void unknownToolThrowsByDefault() {
        ScriptedChatModel model = new ScriptedChatModel(request ->
                ToolScripts.toolCall("getWeatherNow", "{}"));

        ToolAssistant assistant = AiServices.builder(ToolAssistant.class)
                .chatModel(model)
                .tools(weatherTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> assistant.chat(1L, "北京天气怎么样"));

        assertTrue(exception.getMessage().contains("getWeatherNow"), exception.getMessage());
        assertTrue(exception.getMessage().contains("no such tool exists"), exception.getMessage());
    }

    @Test
    void customStrategyFeedsCorrectionBackInsteadOfThrowing() {
        ScriptedChatModel model = new ScriptedChatModel(request -> {
            if (ToolScripts.lastToolResult(request) != null) {
                return ToolScripts.text("已纠正：" + ToolScripts.lastToolResult(request).text());
            }
            return ToolScripts.toolCall("getWeatherNow", "{}");
        });

        ToolAssistant assistant = AiServices.builder(ToolAssistant.class)
                .chatModel(model)
                .tools(weatherTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .hallucinatedToolNameStrategy(request -> ToolExecutionResultMessage.from(
                        request, "工具 " + request.name() + " 不存在，请改用 getWeather"))
                .build();

        Result<String> result = assistant.chat(1L, "北京天气怎么样");

        assertEquals(2, model.requests().size(), "兜底消息回填后应再请求一次模型");
        assertTrue(result.content().contains("工具 getWeatherNow 不存在"));
        assertEquals(1, result.toolExecutions().size(),
                "幻觉工具也会留下一条执行记录，但结果来自兜底策略而不是真实方法");
        assertTrue(result.toolExecutions().get(0).result().contains("不存在"));
        assertNull(weatherTools.lastMemoryId(), "幻觉工具名不会触达真实工具方法");
    }

    @Test
    void sequentialToolLimitStopsEndlessLoop() {
        ScriptedChatModel model = new ScriptedChatModel(request ->
                ToolScripts.toolCall("getWeather", "{\"city\":\"北京\"}"));

        ToolAssistant assistant = AiServices.builder(ToolAssistant.class)
                .chatModel(model)
                .tools(weatherTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .maxSequentialToolsInvocations(1)
                .build();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> assistant.chat(1L, "北京天气怎么样"));

        assertTrue(exception.getMessage().contains("exceeded 1 sequential tool invocations"),
                exception.getMessage());
        assertEquals(2, model.requests().size(),
                "执行完一轮工具后框架已发起第二次推理，随后在下一轮检查处抛错");
    }

}
