package com.caspian.springboot;

import com.caspian.springboot.controller.ChatController;
import com.caspian.springboot.dto.ChatRequest;
import com.caspian.springboot.service.AssistantService;
import com.caspian.springboot.support.FakeStreamingChatModel;
import com.caspian.springboot.support.TestModelsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * day07 Spring Boot 集成：普通问答与 SSE 流式接口的离线验证。
 * <p>
 * 流式部分断言两件事：token 全部推完，以及流以显式的 {@code done} 事件结束，
 * 而不是靠连接断开让客户端猜测；异常路径则必须落到 {@code error} 事件并正常收尾。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestModelsConfig.class)
class ChatControllerSseTest {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    private WebTestClient webTestClient;

    /**
     * 普通问答接口返回模型文本。
     */
    @Test
    void chatEndpointReturnsModelText() {
        String reply = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/springboot/chat")
                        .queryParam("message", "你好")
                        .queryParam("conversationId", "http-check")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertTrue(reply != null && reply.contains("你好"));
    }

    /**
     * SSE 接口逐段推送 token，最后补一个 {@code done} 事件并结束流。
     */
    @Test
    void sseStreamsAllTokensAndClosesWithDoneEvent() {
        List<ServerSentEvent<String>> events = webTestClient.post()
                .uri("/springboot/chat/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequest("流式测试", "sse-check"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(SSE_TYPE)
                .getResponseBody()
                .collectList()
                .block();

        assertTrue(events != null && !events.isEmpty(), "SSE 应至少推送一个事件");

        String streamedText = events.subList(0, events.size() - 1).stream()
                .map(ServerSentEvent::data)
                .collect(Collectors.joining());
        assertEquals(FakeStreamingChatModel.STREAMING_REPLY, streamedText);

        ServerSentEvent<String> last = events.get(events.size() - 1);
        assertEquals("done", last.event(), "最后一个事件应为 done 标记");
        assertEquals("[DONE]", last.data());
    }

    /**
     * 消息为空时不调用模型，直接推送一个 error 事件并结束。
     */
    @Test
    void blankMessageProducesErrorEvent() {
        List<ServerSentEvent<String>> events = webTestClient.post()
                .uri("/springboot/chat/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequest("  ", "sse-check"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(SSE_TYPE)
                .getResponseBody()
                .collectList()
                .block();

        assertTrue(events != null && events.size() == 1);
        assertEquals("error", events.get(0).event());
        assertTrue(events.get(0).data().contains("消息不能为空"));
    }

    /**
     * 模型流抛异常时，控制器把它转成 error 事件并正常结束，客户端不会一直挂起。
     */
    @Test
    void streamingErrorProducesErrorEventAndCompletes() {
        AssistantService failingService = new AssistantService() {
            @Override
            public String chat(String conversationId, String message) {
                return "";
            }

            @Override
            public Flux<String> chatStream(String conversationId, String message) {
                return Flux.error(new IllegalStateException("模型不可用"));
            }
        };
        WebTestClient client = WebTestClient.bindToController(new ChatController(failingService)).build();

        List<ServerSentEvent<String>> events = client.post()
                .uri("/springboot/chat/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequest("会失败的请求", "sse-error"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(SSE_TYPE)
                .getResponseBody()
                .collectList()
                .block();

        assertTrue(events != null && events.size() == 1);
        assertEquals("error", events.get(0).event());
        assertTrue(events.get(0).data().contains("模型不可用"));
    }

}
