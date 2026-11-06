package com.caspian.chatdashscope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * day03 集成百炼平台：Spring 容器加载测试。
 * <p>
 * 只验证自动装配是否成功，不发起任何模型调用；默认跳过，
 * 设置 {@code LIVE_API_TESTS=true} 并提供 DASHSCOPE_API_KEY 后才会执行。
 * </p>
 *
 * @author caspian
 * @since 2026-11-02
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "LIVE_API_TESTS", matches = "true")
class ChatDashscopeApplicationTests {

    @Test
    void contextLoads() {
    }

}
