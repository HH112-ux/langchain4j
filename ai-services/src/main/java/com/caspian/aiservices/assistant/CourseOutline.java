package com.caspian.aiservices.assistant;

import java.util.List;

/**
 * day04 AiServices：结构化输出的目标类型。
 * <p>
 * 模型返回 JSON 文本后由 AiService 自动反序列化成该 record，
 * 调用方拿到的就是对象而不是字符串。
 *
 * @param title    课程标题
 * @param chapters 章节目录
 * @author Caspian
 * @since 2026-11-02
 */
public record CourseOutline(String title, List<String> chapters) {
}
