package com.caspian.chatmemory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * day05 记忆：Spring Boot 启动类。
 * <p>
 * 启动后由 dashscope starter 装配 Qwen 聊天模型，再由 {@code MemoryConfig}
 * 把模型、{@code ChatMemoryProvider} 和 {@code ChatMemoryStore} 组装成声明式助手。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
@SpringBootApplication
public class ChatMemoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatMemoryApplication.class, args);
    }

}
