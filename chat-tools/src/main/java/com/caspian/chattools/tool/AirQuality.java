package com.caspian.chattools.tool;

/**
 * day06 工具调用：空气质量查询结果。
 * <p>
 * 作为工具方法的返回值，框架会把它序列化成 JSON 字符串再回填给模型，
 * 与返回拼接好的 {@code String} 相比，字段边界更清晰、模型也更容易稳定解析。
 * </p>
 *
 * @param city  城市名称
 * @param aqi   空气质量指数
 * @param level 空气质量等级
 * @author Caspian
 * @since 2026-11-08
 */
public record AirQuality(String city, int aqi, String level) {
}
