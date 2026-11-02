package com.caspian.chatdashscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * day03 集成百炼平台：Spring Boot 启动类。
 * <p>
 * 启动后由 langchain4j-community-dashscope-spring-boot-starter 依据 application.yaml
 * 自动装配 Qwen 聊天模型、流式聊天模型与万相文生图模型。
 * </p>
 *
 * @author caspian
 * @since 2026-11-02
 */
@SpringBootApplication
public class ChatDashscopeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatDashscopeApplication.class, args);
    }

}
