package com.caspian.chattools;

import com.caspian.chattools.service.ToolAssistant;
import com.caspian.chattools.support.ScriptedChatModel;
import com.caspian.chattools.support.ToolScripts;
import com.caspian.chattools.tool.WeatherTools;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day06 工具调用：高级别 {@code @Tool} 链路的行为测试。
 * <p>
 * 全部使用离线假模型，验证工具规格生成、工具执行、结果回填与返回类型转换。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
class ToolAssistantTest {

    private final WeatherTools weatherTools = new WeatherTools();

    @Test
    void toolIsExecutedAndResultIsFedBack() {
        ScriptedChatModel model = toolThenAnswer("getWeather", "{\"city\":\"北京\",\"unit\":\"celsius\"}");

        Result<String> result = assistant(model).chat(1L, "北京天气怎么样");

        assertEquals(2, model.requests().size(), "一次工具调用需要两轮模型请求");
        assertEquals(1, result.toolExecutions().size());
        ToolExecution execution = result.toolExecutions().get(0);
        assertEquals("getWeather", execution.request().name());
        assertTrue(execution.result().contains("北京"), "工具结果应回填给模型");
        assertTrue(result.content().contains("北京"), "最终回答应基于工具结果");
        assertEquals(1L, weatherTools.lastMemoryId(), "@ToolMemoryId 应传入会话标识");
    }

    @Test
    void toolSchemaExcludesMemoryIdAndMarksOptionalParameter() {
        ScriptedChatModel model = toolThenAnswer("getWeather", "{\"city\":\"北京\"}");

        assistant(model).chat(7L, "北京天气怎么样");

        ToolSpecification spec = model.requestAt(0).toolSpecifications().stream()
                .filter(specification -> "getWeather".equals(specification.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(spec.description().contains("天气"), "工具描述是给模型看的说明书");
        assertTrue(spec.parameters().properties().containsKey("city"));
        assertTrue(spec.parameters().properties().containsKey("unit"));
        assertFalse(spec.parameters().properties().containsKey("memoryId"),
                "@ToolMemoryId 是框架注入的上下文参数，不应进入 schema");
        assertEquals(List.of("city"), spec.parameters().required(),
                "只有 city 必填，unit 标注了 required = false");
    }

    @Test
    void optionalParameterFallsBackWhenModelOmitsIt() {
        ScriptedChatModel model = toolThenAnswer("getWeather", "{\"city\":\"上海\"}");

        Result<String> result = assistant(model).chat(2L, "上海天气怎么样");

        assertTrue(result.toolExecutions().get(0).result().contains("单位 celsius"));
        assertEquals(2L, weatherTools.lastMemoryId());
    }

    @Test
    void pojoReturnValueIsSerializedAsJson() {
        ScriptedChatModel model = toolThenAnswer("getAirQuality", "{\"city\":\"北京\"}");

        Result<String> result = assistant(model).chat(1L, "北京空气质量如何");

        String toolResult = result.toolExecutions().get(0).result();
        assertTrue(toolResult.startsWith("{"), "record 返回值应转成 JSON 字符串：" + toolResult);
        assertTrue(toolResult.contains("\"aqi\":42"), toolResult);
        assertTrue(toolResult.contains("优"), toolResult);
    }

    @Test
    void voidToolReturnsSuccess() {
        ScriptedChatModel model = toolThenAnswer("rememberPreference", "{\"preference\":\"喜欢晴天\"}");

        Result<String> result = assistant(model).chat(1L, "记住我喜欢晴天");

        assertEquals("Success", result.toolExecutions().get(0).result(),
                "void 返回值固定回填 Success");
        assertEquals(List.of("喜欢晴天"), weatherTools.preferences(), "工具方法本身应真正执行");
    }

    /**
     * 构造"第一次请求工具、第二次基于工具结果回答"的假模型。
     *
     * @param toolName  工具名
     * @param arguments 工具参数 JSON
     * @return 假模型
     */
    private ScriptedChatModel toolThenAnswer(String toolName, String arguments) {
        return new ScriptedChatModel(request -> {
            if (ToolScripts.lastToolResult(request) != null) {
                return ToolScripts.text("最终回答：" + ToolScripts.lastToolResult(request).text());
            }
            return ToolScripts.toolCall(toolName, arguments);
        });
    }

    /**
     * 构造使用静态工具实例的助手。
     *
     * @param model 假模型
     * @return 助手
     */
    private ToolAssistant assistant(ScriptedChatModel model) {
        InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
        ChatMemoryProvider provider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(store)
                .build();
        return AiServices.builder(ToolAssistant.class)
                .chatModel(model)
                .tools(weatherTools)
                .chatMemoryProvider(provider)
                .build();
    }

}
