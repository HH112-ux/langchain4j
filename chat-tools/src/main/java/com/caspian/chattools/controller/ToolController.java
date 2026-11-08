package com.caspian.chattools.controller;

import com.caspian.chattools.service.DynamicToolAssistant;
import com.caspian.chattools.service.ToolAssistant;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * day06 工具调用：手动验证工具链路的 HTTP 入口。
 * <p>
 * {@code /tools/chat} 走静态 {@code @Tool} 工具，
 * {@code /tools/dynamic} 走按请求动态暴露的工具。
 * 返回体同时给出最终回答和本次实际执行的工具名，方便确认模型是否真的调用了工具。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/tools")
public class ToolController {

    private final ToolAssistant toolAssistant;

    private final DynamicToolAssistant dynamicToolAssistant;

    /**
     * @param toolAssistant        静态工具助手
     * @param dynamicToolAssistant 动态工具助手
     */
    public ToolController(ToolAssistant toolAssistant, DynamicToolAssistant dynamicToolAssistant) {
        this.toolAssistant = toolAssistant;
        this.dynamicToolAssistant = dynamicToolAssistant;
    }

    /**
     * 使用静态注册的工具回答问题。
     *
     * @param memoryId 会话标识
     * @param message  用户输入
     * @return 最终回答与实际执行的工具名
     */
    @GetMapping("/chat")
    public ToolChatView chat(@RequestParam long memoryId, @RequestParam String message) {
        return toView(toolAssistant.chat(memoryId, message));
    }

    /**
     * 使用动态提供者暴露的工具回答问题。
     *
     * @param memoryId 会话标识
     * @param message  用户输入
     * @return 最终回答与实际执行的工具名
     */
    @GetMapping("/dynamic")
    public ToolChatView dynamic(@RequestParam long memoryId, @RequestParam String message) {
        return toView(dynamicToolAssistant.chat(memoryId, message));
    }

    /**
     * 把框架结果裁剪成适合展示的视图。
     *
     * @param result 助手返回结果
     * @return 视图对象
     */
    private ToolChatView toView(Result<String> result) {
        List<String> toolNames = result.toolExecutions().stream()
                .map(ToolExecution::request)
                .map(request -> request.name())
                .toList();
        return new ToolChatView(result.content(), toolNames);
    }

    /**
     * 工具调用结果的展示结构。
     *
     * @param answer 最终回答
     * @param tools  实际执行的工具名
     */
    public record ToolChatView(String answer, List<String> tools) {
    }

}
