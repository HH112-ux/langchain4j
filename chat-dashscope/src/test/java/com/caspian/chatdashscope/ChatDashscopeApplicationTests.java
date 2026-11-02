package com.caspian.chatdashscope;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * day03 集成百炼平台：Spring 容器加载测试。
 * <p>
 * 只验证自动装配是否成功，不发起任何模型调用；运行前需要提供 DASHSCOPE_API_KEY。
 * </p>
 *
 * @author caspian
 * @since 2026-11-02
 */
@SpringBootTest
class ChatDashscopeApplicationTests {

    @Test
    void contextLoads() {
    }

}
