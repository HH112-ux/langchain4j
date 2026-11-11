package com.caspian.springboot.controller;

import com.caspian.springboot.dto.ChatRequest;
import com.caspian.springboot.service.AssistantService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * day07 Spring Boot 集成：对外暴露的聊天接口。
 * <p>
 * 应用是 WebFlux（父 POM 已引入 {@code spring-boot-starter-webflux}），
 * 所以流式接口返回 {@code Flux<ServerSentEvent<String>>}，由框架写成
 * {@code text/event-stream}；普通接口仍旧同步返回字符串。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-11
 */
@RestController
@RequestMapping("/springboot")
public class ChatController {

    /**
     * 流式接口结束时下发的哨兵事件，客户端据此判断“真的结束”，
     * 而不是依赖连接断开。
     */
    private static final String DONE_MARKER = "[DONE]";

    /**
     * 会话标识为空时的默认值，保证未传 conversationId 的调用也能命中同一份记忆。
     */
    private static final String DEFAULT_CONVERSATION_ID = "default";

    private final AssistantService assistantService;

    /**
     * @param assistantService 由 starter 扫描生成的声明式服务代理
     */
    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * 普通问答。
     *
     * @param message        用户问题，必填
     * @param conversationId 会话标识，默认 {@code default}，用于隔离记忆
     * @return 模型的完整回答
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message,
                       @RequestParam(defaultValue = DEFAULT_CONVERSATION_ID) String conversationId) {
        return assistantService.chat(conversationId, message);
    }

    /**
     * SSE 流式问答。
     * <p>
     * 每个 token 作为一个事件推送，全部结束后追加一个 {@code event: done} 的
     * {@value #DONE_MARKER} 事件；模型或网络异常时改为推送一个 {@code event: error}
     * 事件，然后正常结束流，避免客户端一直等待。
     * </p>
     *
     * @param request 请求体，包含问题与会话标识
     * @return SSE 事件流，响应类型为 {@code text/event-stream}
     */
    @PostMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatSse(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return Flux.just(errorEvent("[错误]：消息不能为空"));
        }
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? DEFAULT_CONVERSATION_ID
                : request.conversationId();
        Flux<ServerSentEvent<String>> tokens = assistantService.chatStream(conversationId, request.message())
                .map(token -> ServerSentEvent.builder(token).build());
        Flux<ServerSentEvent<String>> done = Flux.just(ServerSentEvent.builder(DONE_MARKER)
                .event("done")
                .build());
        return Flux.concat(tokens, done)
                .onErrorResume(error -> Flux.just(errorEvent("[错误]：" + error.getMessage())));
    }

    /**
     * 构造一个错误事件。
     *
     * @param text 推送给客户端的错误文本
     * @return 带 {@code error} 事件名的 SSE 事件
     */
    private ServerSentEvent<String> errorEvent(String text) {
        return ServerSentEvent.builder(text).event("error").build();
    }

}
