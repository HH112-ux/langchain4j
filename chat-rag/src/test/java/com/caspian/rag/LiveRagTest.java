package com.caspian.rag;

import com.caspian.rag.dto.RagAnswer;
import com.caspian.rag.service.RagAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day08 RAG：真实模型的联网验证。
 * <p>
 * 默认跳过；设置 {@code LIVE_API_TESTS=true} 并提供 {@code DASHSCOPE_API_KEY} 后，
 * 用真实的 qwen-plus 与 text-embedding-v4 跑完整链路。启动时会调用 embedding 接口。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
@EnabledIfEnvironmentVariable(named = "LIVE_API_TESTS", matches = "true")
@SpringBootTest
class LiveRagTest {

    @Autowired
    private RagAssistant ragAssistant;

    /**
     * 真实链路应检索到示例文档并给出非空回答。
     */
    @Test
    void answersWithRetrievedContext() {
        RagAnswer answer = ragAssistant.chat("差旅报销需要哪些材料？");

        assertTrue(answer.matched(), "示例文档应能命中");
        assertFalse(answer.answer().isBlank());
    }

}
