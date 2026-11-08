package com.caspian.chattools.provider;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

/**
 * day06 工具调用：按请求动态提供工具的示例。
 * <p>
 * 只有用户消息里出现"北京"时，才向模型暴露 {@code getBeijingWeather}；
 * 其他请求返回 {@code null}，表示本次不提供任何工具。
 * 这样可以按租户、权限或意图裁剪工具清单，避免把所有工具都塞给模型。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
public class BeijingWeatherToolProvider implements ToolProvider {

    /**
     * 动态生成的工具描述：无参数，因此参数 schema 是个空对象。
     */
    private static final ToolSpecification BEIJING_WEATHER_SPEC = ToolSpecification.builder()
            .name("getBeijingWeather")
            .description("查询北京当前天气，无参数，返回天气现象与气温")
            .parameters(JsonObjectSchema.builder().build())
            .build();

    /**
     * 根据用户输入决定本次暴露的工具。
     *
     * @param request 本次请求上下文，可拿到用户消息与会话 ID
     * @return 北京相关请求返回一个工具，其余返回 {@code null}
     */
    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        if (!request.userMessage().singleText().contains("北京")) {
            return null;
        }
        return ToolProviderResult.builder()
                .add(BEIJING_WEATHER_SPEC, (toolRequest, memoryId) ->
                        "北京天气晴，气温 26°C，会话 " + memoryId)
                .build();
    }

}
