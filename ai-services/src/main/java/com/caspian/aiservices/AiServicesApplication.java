package com.caspian.aiservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * day04 AiServices：Spring Boot 启动类。
 * <p>
 * 启动后由 dashscope starter 自动装配 Qwen 聊天模型与流式聊天模型，
 * 再由 {@code AiServicesConfig} 把模型装配成声明式的 {@code Assistant}。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-02
 */
@SpringBootApplication
public class AiServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServicesApplication.class, args);
    }

}
