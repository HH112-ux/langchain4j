package com.caspian.springboot.support;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * day07 Spring Boot 集成：测试用模型装配。
 * <p>
 * bean 名必须与 {@code @AiService} 里 EXPLICIT 模式引用的名字一致，
 * 否则 starter 生成的代理找不到依赖；类型同时满足 {@link ChatModel} 与
 * {@link StreamingChatModel} 的注入要求。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
@TestConfiguration
public class TestModelsConfig {

    /**
     * @return 顶替 DashScope 自动配置的非流式模型
     */
    @Bean("qwenChatModel")
    public FakeChatModel qwenChatModel() {
        return new FakeChatModel();
    }

    /**
     * @return 顶替 DashScope 自动配置的流式模型
     */
    @Bean("qwenStreamingChatModel")
    public FakeStreamingChatModel qwenStreamingChatModel() {
        return new FakeStreamingChatModel();
    }

}
