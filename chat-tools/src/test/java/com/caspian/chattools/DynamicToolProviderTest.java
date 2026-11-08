package com.caspian.chattools;

import com.caspian.chattools.provider.BeijingWeatherToolProvider;
import com.caspian.chattools.service.DynamicToolAssistant;
import com.caspian.chattools.support.ScriptedChatModel;
import com.caspian.chattools.support.ToolScripts;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day06 工具调用：按请求动态提供工具的行为测试。
 *
 * @author Caspian
 * @since 2026-11-08
 */
class DynamicToolProviderTest {

    @Test
    void beijingRequestExposesDynamicTool() {
        ScriptedChatModel model = dynamicModel();

        Result<String> result = assistant(model).chat(1L, "北京天气怎么样");

        assertEquals(1, result.toolExecutions().size());
        assertEquals("getBeijingWeather", result.toolExecutions().get(0).request().name());
        assertTrue(result.toolExecutions().get(0).result().contains("会话 1"));
        assertEquals(1, model.requestAt(0).toolSpecifications().size(),
                "命中北京时动态暴露一个工具");
    }

    @Test
    void otherCityGetsNoTool() {
        ScriptedChatModel model = dynamicModel();

        Result<String> result = assistant(model).chat(2L, "上海天气怎么样");

        assertTrue(result.toolExecutions().isEmpty(), "provider 返回 null 时本次没有工具可调");
        assertEquals("没有可用工具，直接回答", result.content());
        assertTrue(model.requestAt(0).toolSpecifications().isEmpty(),
                "未命中时不应向模型暴露任何工具");
        assertEquals(1, model.requests().size(), "没有工具调用时只需要一轮请求");
    }

    /**
     * 动态场景下的假模型：有工具且没回过结果就请求工具，否则直接回答。
     *
     * @return 假模型
     */
    private ScriptedChatModel dynamicModel() {
        return new ScriptedChatModel(request -> {
            if (ToolScripts.lastToolResult(request) != null) {
                return ToolScripts.text("最终回答：" + ToolScripts.lastToolResult(request).text());
            }
            if (!request.toolSpecifications().isEmpty()) {
                return ToolScripts.toolCall("getBeijingWeather", "{}");
            }
            return ToolScripts.text("没有可用工具，直接回答");
        });
    }

    /**
     * @param model 假模型
     * @return 只挂动态工具提供者的助手
     */
    private DynamicToolAssistant assistant(ScriptedChatModel model) {
        return AiServices.builder(DynamicToolAssistant.class)
                .chatModel(model)
                .toolProvider(new BeijingWeatherToolProvider())
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

}
