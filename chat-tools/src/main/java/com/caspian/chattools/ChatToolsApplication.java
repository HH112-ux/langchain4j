package com.caspian.chattools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * day06 工具调用：Spring Boot 启动类。
 * <p>
 * 启动后由 dashscope starter 装配 Qwen 聊天模型，再由 {@code ToolConfig}
 * 把模型、工具实例和记忆装配成声明式助手，演示"模型表达意图、应用本地执行"的完整链路。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
@SpringBootApplication
public class ChatToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatToolsApplication.class, args);
    }

}
