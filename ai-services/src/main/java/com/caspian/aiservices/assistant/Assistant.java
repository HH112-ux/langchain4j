package com.caspian.aiservices.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * day04 AiServices：声明式助手接口。
 * <p>
 * 接口本身没有实现，由 {@code AiServices.builder} 在运行时生成代理对象，
 * 负责把方法参数转成 ChatMessage、再把模型回复转回方法返回类型。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-02
 */
public interface Assistant {

    /**
     * 带固定系统指令的单轮问答。
     *
     * @param userMessage 用户提问
     * @return 模型回复文本
     */
    @SystemMessage("你是一位严谨的 Java 讲师，回答控制在三句话以内。")
    String chat(String userMessage);

    /**
     * 用 {@code {{topic}}} 占位符把方法参数注入用户消息模板。
     *
     * @param topic 要讲解的主题
     * @return 模型回复文本
     */
    @UserMessage("用两句话说明 {{topic}} 的核心作用。")
    String explain(@V("topic") String topic);

}
