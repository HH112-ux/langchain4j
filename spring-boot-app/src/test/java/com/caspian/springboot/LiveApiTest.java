package com.caspian.springboot;

import com.caspian.springboot.service.AssistantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * day07 Spring Boot 集成：真实模型的联网验证。
 * <p>
 * 默认跳过；设置 {@code LIVE_API_TESTS=true} 并提供 {@code DASHSCOPE_API_KEY} 后，
 * 通过 {@code live} profile 打开 DashScope 自动配置，让 starter 创建真实模型 bean。
 * 这样 {@code mvn -o test} 在离线环境下始终全绿且不计费。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
@EnabledIfEnvironmentVariable(named = "LIVE_API_TESTS", matches = "true")
@ActiveProfiles("live")
@SpringBootTest
class LiveApiTest {

    @Autowired
    private AssistantService assistantService;

    /**
     * 真实模型应返回非空文本，证明 starter + 配置文件 + {@code @AiService} 整条链路可用。
     */
    @Test
    void starterConfiguredServiceAnswersQuestion() {
        assertNotNull(assistantService);

        String reply = assistantService.chat("live-check", "用一句话说明 LangChain4j 是什么。");

        assertFalse(reply.isBlank(), "真实模型应给出回答");
    }

}
