package com.caspian.springboot.dto;

/**
 * day07 Spring Boot 集成：流式接口的请求体。
 * <p>
 * 浏览器端用 {@code fetch} 发 POST，再自行解析 {@code text/event-stream} 响应；
 * 之所以不用原生 {@code EventSource}，是因为它只支持 GET 且不能携带请求体。
 * </p>
 *
 * @param message        用户问题
 * @param conversationId 会话标识，为空时由服务端回退到 {@code default}
 * @author Caspian
 * @since 2026-11-11
 */
public record ChatRequest(String message, String conversationId) {
}
