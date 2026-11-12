package com.caspian.rag;

import com.caspian.rag.dto.RagAnswer;
import com.caspian.rag.dto.RagChatRequest;
import com.caspian.rag.dto.SearchHit;
import com.caspian.rag.support.FakeModelsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day08 RAG：接口层的集成验证。
 * <p>
 * 上下文用 {@code rag.models=fake} 关闭真实模型，启动时用哈希向量摄入示例文档。
 * 为了验证接口而不是相似度分布，测试把 {@code min-score} 调到 0，
 * 生产默认值仍是配置里的 0.5。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"rag.models=fake", "rag.retrieval.min-score=0.0"})
@AutoConfigureWebTestClient
@Import(FakeModelsConfig.class)
class RagControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * 问答接口返回回答与来源，说明检索确实参与了生成。
     */
    @Test
    void chatReturnsAnswerWithSources() {
        RagAnswer answer = webTestClient.post()
                .uri("/rag/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RagChatRequest("差旅报销需要哪些材料"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RagAnswer.class)
                .returnResult()
                .getResponseBody();

        assertTrue(answer != null && answer.matched());
        assertFalse(answer.answer().isBlank());
        assertFalse(answer.sources().isEmpty());
    }

    /**
     * 调试接口只返回检索命中，不调用模型。
     */
    @Test
    void searchReturnsHits() {
        List<SearchHit> hits = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/rag/search").queryParam("q", "报销").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<SearchHit>>() {
                })
                .returnResult()
                .getResponseBody();

        assertTrue(hits != null && !hits.isEmpty());
        assertTrue(hits.get(0).text().contains("报销"));
    }

    /**
     * 空问题直接 400，不进入检索与生成。
     */
    @Test
    void blankQuestionIsRejected() {
        webTestClient.post()
                .uri("/rag/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RagChatRequest("  "))
                .exchange()
                .expectStatus().isBadRequest();
    }

}
