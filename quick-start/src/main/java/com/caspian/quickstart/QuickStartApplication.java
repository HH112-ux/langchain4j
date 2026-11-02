package com.caspian.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * day02 快速入门：Spring Boot 启动类。
 * <p>
 * LangChain4j 第一个 Hello World 工程的入口。
 * </p>
 *
 * @author caspian
 * @since 2026-11-02
 */
@SpringBootApplication
public class QuickStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickStartApplication.class, args);
        System.out.println("=========启动成功==========");
    }

}
