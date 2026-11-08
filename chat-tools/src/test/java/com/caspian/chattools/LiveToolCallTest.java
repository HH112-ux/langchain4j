package com.caspian.chattools;

import com.caspian.chattools.service.ToolAssistant;
import com.caspian.chattools.tool.WeatherTools;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * day06 工具调用：真实模型的联网验证。
 * <p>
 * 默认跳过；设置 {@code LIVE_API_TESTS=true} 并提供 {@code DASHSCOPE_API_KEY}
 * 后才会计费调用百炼服务，保证 {@code mvn -o test} 在离线环境下始终为绿。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
@EnabledIfEnvironmentVariable(named = "LIVE_API_TESTS", matches = "true")
class LiveToolCallTest {

    @Test
    void qwenChoosesWeatherToolForWeatherQuestion() {
        QwenChatModel chatModel = QwenChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen-plus")
                .temperature(0.3f)
                .build();

        ToolAssistant assistant = AiServices.builder(ToolAssistant.class)
                .chatModel(chatModel)
                .tools(new WeatherTools())
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        Result<String> result = assistant.chat(1L, "北京现在天气怎么样？");

        assertFalse(result.content().isBlank(), "真实模型应给出回答");
        assertFalse(result.toolExecutions().isEmpty(), "天气问题应触发 getWeather 工具");
    }

}
