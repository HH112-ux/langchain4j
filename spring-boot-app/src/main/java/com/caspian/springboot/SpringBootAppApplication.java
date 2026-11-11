package com.caspian.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * day07 Spring Boot 集成 LangChain4j：应用入口。
 * <p>
 * 与 day04 手写的 {@code AiServicesConfig} 不同，这里的 {@code ChatModel}、
 * {@code StreamingChatModel} 和 {@code @AiService} 代理都由 starter 的自动配置产生，
 * 本类只负责启动容器。配置项见 {@code application.yaml} 的 {@code langchain4j.community.dashscope.*}。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
@SpringBootApplication
public class SpringBootAppApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringBootAppApplication.class, args);
    }

}
