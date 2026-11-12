package com.caspian.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * day08 RAG：应用入口。
 * <p>
 * 本模块演示最小可运行的 RAG 链路：启动时把小文档切分、向量化后写入内存向量库，
 * 问答时先检索再生成。模型与重排都用本模块的配置装配，不依赖 starter 的自动配置。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
@SpringBootApplication
public class RagApplication {

    /**
     * 启动应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }

}
