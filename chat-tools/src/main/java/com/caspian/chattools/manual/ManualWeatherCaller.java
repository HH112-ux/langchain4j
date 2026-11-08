package com.caspian.chattools.manual;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * day06 工具调用：低级别手动链路。
 * <p>
 * 不走 {@code @Tool} 和 AiServices，完整手写两轮请求：
 * 第一轮带上 {@link ToolSpecification}，模型若返回工具调用意图，
 * 由本类解析参数、执行本地逻辑，再把 {@link ToolExecutionResultMessage}
 * 与第一轮 AI 消息一起放进第二轮请求，最终拿到自然语言回答。
 * </p>
 * <p>
 * 这条链路是用来理解框架内部做了什么，实际项目优先用 {@code @Tool}。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
public class ManualWeatherCaller {

    /**
     * 手动构造的天气工具描述。
     */
    private static final ToolSpecification WEATHER_SPEC = ToolSpecification.builder()
            .name("getWeather")
            .description("查询指定城市的天气预报")
            .parameters(JsonObjectSchema.builder()
                    .addStringProperty("city", "要查询天气预报的城市名称")
                    .required("city")
                    .build())
            .build();

    private final ChatModel chatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param chatModel 聊天模型，测试时可传入离线假模型
     */
    public ManualWeatherCaller(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 走完"请求工具 → 本地执行 → 回填结果 → 再次请求"的两轮链路。
     *
     * @param question 用户问题
     * @return 模型基于工具结果的最终回答
     */
    public String ask(String question) {
        UserMessage userMessage = UserMessage.from(question);
        ChatResponse firstResponse = chatModel.chat(ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(WEATHER_SPEC)
                .build());

        AiMessage aiMessage = firstResponse.aiMessage();
        if (!aiMessage.hasToolExecutionRequests()) {
            return aiMessage.text();
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(userMessage);
        messages.add(aiMessage);
        for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
            messages.add(ToolExecutionResultMessage.from(toolRequest, executeWeatherTool(toolRequest)));
        }

        ChatResponse secondResponse = chatModel.chat(ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(WEATHER_SPEC)
                .build());
        return secondResponse.aiMessage().text();
    }

    /**
     * 本地执行工具：从 arguments 中取出城市，返回假天气。
     *
     * @param toolRequest 模型给出的工具调用请求
     * @return 能被回填给模型的结果文本
     */
    private String executeWeatherTool(ToolExecutionRequest toolRequest) {
        try {
            String city = objectMapper.readTree(toolRequest.arguments()).path("city").asText("未知城市");
            return "预计" + city + "明天有小雨，气温 18°C。";
        } catch (JsonProcessingException e) {
            return "天气工具参数解析失败：" + e.getOriginalMessage();
        }
    }

}
