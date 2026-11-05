package com.caspian.aiservices.config;

import com.caspian.aiservices.assistant.Assistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * day04 AiServices：把底层模型装配成声明式接口的配置类。
 *
 * @author Caspian
 * @since 2026-11-02
 */
@Configuration
public class AiServicesConfig {

    /**
     * 用 {@link AiServices#builder(Class)} 为 {@link Assistant} 生成代理实现。
     * <p>
     * 记忆使用 10 条消息的滑动窗口；同时装配流式模型以支持 TokenStream 方法。
     *
     * @param qwenChatModel          由 starter 依据 application.yaml 装配的非流式聊天模型
     * @param qwenStreamingChatModel 同一份配置装配出的流式聊天模型
     * @return 可直接注入使用的声明式助手
     */
    @Bean
    public Assistant assistant(QwenChatModel qwenChatModel,
                               QwenStreamingChatModel qwenStreamingChatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

}
