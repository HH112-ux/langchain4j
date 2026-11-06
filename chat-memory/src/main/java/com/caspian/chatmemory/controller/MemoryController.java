package com.caspian.chatmemory.controller;

import com.caspian.chatmemory.service.ChatPersistenceAssistant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * day05 记忆：手动验证多会话隔离的 HTTP 入口。
 * <p>
 * 同一 memoryId 连续请求会带上历史，不同 memoryId 之间互不影响。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-06
 */
@RestController
@RequestMapping("/memory")
public class MemoryController {

    private final ChatPersistenceAssistant chatPersistenceAssistant;

    /**
     * @param chatPersistenceAssistant 多会话助手
     */
    public MemoryController(ChatPersistenceAssistant chatPersistenceAssistant) {
        this.chatPersistenceAssistant = chatPersistenceAssistant;
    }

    /**
     * 在指定会话中发送一条消息。
     *
     * @param memoryId 会话标识
     * @param message  用户输入
     * @return 模型回复
     */
    @GetMapping("/chat")
    public String chat(@RequestParam long memoryId, @RequestParam String message) {
        return chatPersistenceAssistant.chat(memoryId, message);
    }

}
