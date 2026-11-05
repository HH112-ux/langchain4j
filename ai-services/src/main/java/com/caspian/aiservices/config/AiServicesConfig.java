package com.caspian.aiservices.config;

import com.caspian.aiservices.assistant.Assistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
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
     *
     * @param qwenChatModel 由 starter 依据 application.yaml 装配好的聊天模型
     * @return 可直接注入使用的声明式助手
     */
    @Bean
    public Assistant assistant(QwenChatModel qwenChatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(qwenChatModel)
                .build();
    }

}
