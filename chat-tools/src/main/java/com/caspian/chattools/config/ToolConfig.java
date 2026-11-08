package com.caspian.chattools.config;

import com.caspian.chattools.manual.ManualWeatherCaller;
import com.caspian.chattools.provider.BeijingWeatherToolProvider;
import com.caspian.chattools.service.DynamicToolAssistant;
import com.caspian.chattools.service.ToolAssistant;
import com.caspian.chattools.tool.WeatherTools;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * day06 工具调用：把模型、工具和记忆装配成助手。
 * <p>
 * 两套装配分别演示两种工具来源：静态注册的 {@link WeatherTools} 实例，
 * 以及按请求动态产出规格的 {@link BeijingWeatherToolProvider}。
 * 同时配置了连续工具调用上限与幻觉工具名兜底，避免模型陷入循环或直接抛异常。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
@Configuration
public class ToolConfig {

    /**
     * 每个会话保留的消息条数。
     */
    private static final int MAX_MESSAGES = 20;

    /**
     * 单次问答允许的最大连续工具调用次数，防止模型反复调用。
     */
    private static final int MAX_SEQUENTIAL_TOOLS = 4;

    /**
     * 静态工具助手的记忆提供者，独立一份内存存储。
     *
     * @return 按会话隔离的记忆提供者
     */
    @Bean
    public ChatMemoryProvider toolChatMemoryProvider() {
        return memoryProvider(new InMemoryChatMemoryStore());
    }

    /**
     * 动态工具助手的记忆提供者，与静态助手互不干扰。
     *
     * @return 按会话隔离的记忆提供者
     */
    @Bean
    public ChatMemoryProvider dynamicToolChatMemoryProvider() {
        return memoryProvider(new InMemoryChatMemoryStore());
    }

    /**
     * 装配静态工具助手：{@code @Tool} 方法由框架自动转规格、自动执行、自动回填。
     *
     * @param chatModel  由 starter 装配的聊天模型
     * @param weatherTools 工具实例
     * @param toolChatMemoryProvider 记忆提供者
     * @return 可直接注入的助手
     */
    @Bean
    public ToolAssistant toolAssistant(QwenChatModel chatModel,
                                       WeatherTools weatherTools,
                                       @Qualifier("toolChatMemoryProvider") ChatMemoryProvider toolChatMemoryProvider) {
        return AiServices.builder(ToolAssistant.class)
                .chatModel(chatModel)
                .tools(weatherTools)
                .chatMemoryProvider(toolChatMemoryProvider)
                .maxSequentialToolsInvocations(MAX_SEQUENTIAL_TOOLS)
                .hallucinatedToolNameStrategy(request -> ToolExecutionResultMessage.from(
                        request,
                        "不存在名为 " + request.name() + " 的工具，请使用已提供的工具或直接回答。"))
                .build();
    }

    /**
     * 装配动态工具助手：不注册任何固定工具，由 provider 每次请求现场决定。
     *
     * @param chatModel 由 starter 装配的聊天模型
     * @param dynamicToolChatMemoryProvider 记忆提供者
     * @return 动态工具助手
     */
    @Bean
    public DynamicToolAssistant dynamicToolAssistant(QwenChatModel chatModel,
                                                     @Qualifier("dynamicToolChatMemoryProvider") ChatMemoryProvider dynamicToolChatMemoryProvider) {
        return AiServices.builder(DynamicToolAssistant.class)
                .chatModel(chatModel)
                .toolProvider(new BeijingWeatherToolProvider())
                .chatMemoryProvider(dynamicToolChatMemoryProvider)
                .maxSequentialToolsInvocations(MAX_SEQUENTIAL_TOOLS)
                .build();
    }

    /**
     * 低级别手动链路的入口。
     *
     * @param chatModel 由 starter 装配的聊天模型
     * @return 手动调用器
     */
    @Bean
    public ManualWeatherCaller manualWeatherCaller(QwenChatModel chatModel) {
        return new ManualWeatherCaller(chatModel);
    }

    /**
     * 构造按会话隔离的窗口记忆提供者。
     *
     * @param store 共享的底层存储
     * @return 记忆提供者
     */
    private ChatMemoryProvider memoryProvider(InMemoryChatMemoryStore store) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(store)
                .build();
    }

}
