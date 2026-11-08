package com.caspian.chattools.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * day06 工具调用：供模型调用的本地 Java 工具。
 * <p>
 * 每个 {@link Tool} 方法都会由框架转换成一份 {@code ToolSpecification}：
 * 方法名是工具名，注解里的文本是工具描述，{@link P} 描述参数并决定是否必填。
 * 模型只能看到这份"说明书"，方法体始终在本地执行。
 * </p>
 * <p>
 * 三个方法分别覆盖三种返回类型：{@code String} 原样回填、record 序列化成 JSON、
 * {@code void} 回填固定文本 {@code "Success"}。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-08
 */
@Component
public class WeatherTools {

    /**
     * 最近一次调用收到的会话 ID，仅用于测试断言 {@code @ToolMemoryId} 的注入结果。
     */
    private final AtomicReference<Long> lastMemoryId = new AtomicReference<>();

    /**
     * 记录到的用户偏好，仅用于测试断言 {@code void} 工具的副作用。
     */
    private final List<String> preferences = new CopyOnWriteArrayList<>();

    /**
     * 查询城市天气，演示可选参数与 {@link ToolMemoryId}。
     * <p>
     * {@code unit} 标了 {@code required = false}，模型可以完全不传；
     * {@code memoryId} 上的 {@link ToolMemoryId} 由框架注入，不会出现在参数 schema 里。
     * </p>
     *
     * @param city     城市名称，必填
     * @param unit     温度单位，可选，默认摄氏度
     * @param memoryId 会话标识，框架注入，必须放在参数列表最后
     * @return 天气描述文本，原样回填给模型
     */
    @Tool("查询指定城市的当前天气。输入城市名称，返回天气现象与气温")
    public String getWeather(
            @P("要查询天气的城市名称，例如：北京") String city,
            @P(value = "温度单位，可选 celsius 或 fahrenheit，不传默认 celsius", required = false) String unit,
            @ToolMemoryId Long memoryId) {
        lastMemoryId.set(memoryId);
        String resolvedUnit = (unit == null || unit.isBlank()) ? "celsius" : unit;
        boolean fahrenheit = "fahrenheit".equalsIgnoreCase(resolvedUnit);
        int temperature = fahrenheit ? 79 : 26;
        return city + " 天气晴，气温 " + temperature + (fahrenheit ? "°F" : "°C")
                + "，单位 " + resolvedUnit + "，会话 " + memoryId;
    }

    /**
     * 查询空气质量，演示返回 record 时框架自动转 JSON。
     *
     * @param city 城市名称
     * @return 空气质量结果对象
     */
    @Tool("查询指定城市的空气质量指数，返回 AQI 数值与等级")
    public AirQuality getAirQuality(@P("要查询空气质量的城市名称，例如：北京") String city) {
        return new AirQuality(city, 42, "优");
    }

    /**
     * 记录用户偏好，演示 {@code void} 返回值。
     *
     * @param preference 偏好描述
     */
    @Tool("记录用户对天气的偏好，例如喜欢晴天或讨厌下雨")
    public void rememberPreference(@P("用户的偏好描述，例如：喜欢晴天") String preference) {
        preferences.add(preference);
    }

    /**
     * @return 最近一次天气查询收到的会话 ID
     */
    public Long lastMemoryId() {
        return lastMemoryId.get();
    }

    /**
     * @return 已记录的偏好列表
     */
    public List<String> preferences() {
        return List.copyOf(preferences);
    }

}
