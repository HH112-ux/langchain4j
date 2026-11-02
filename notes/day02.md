# day02 学习笔记：快速入门（quick-start）

## 做了什么

跑通 LangChain4j 最基础的调用链路：Spring Boot 启动 → 注入 Chat Model → 发一条消息拿回复。然后试了三种调用方式——纯文本、远程图片 URL、本地图片 Base64。

## 三种调用方式对比

| 方法 | 喂给模型的内容 | 适用场景 |
|------|---------------|---------|
| `textTest` | 只传一个 `"你是谁"` 字符串 | 验证模型能通 |
| `imageTest` | `TextContent` + `ImageContent.from(url)` | 图片在公网可访问 |
| `imageTest2` | `ImageContent.from(base64, mime)` + `TextContent` | 本地/内网图片，需要自己读文件→转 Base64 |

关键点：`UserMessage.from()` 可以接受多个 `Content` 对象（text、image…），按顺序放进去就行。多模态模型自己知道怎么处理。

## application.yaml 关键项怎么理解

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: 'demo'
      model-name: 'gpt-4o-mini'
      base-url: 'http://langchain4j.dev/demo/openai/v1'
```

- `base-url`：LangChain4j 官方提供了一个免费 demo endpoint，`api-key: demo` 就能跑 Hello World；真实生产要换 OpenAI 或兼容接口的地址
- `model-name`：`gpt-4o-mini` 便宜且支持多模态（GPT-4o 全家桶都支持图片），所以 day02 用它做图片输入示例
- 三个字段都可以通过 `@Value` 注入手动构建 ChatModel，也可以让 Spring Boot 自动装配（`OpenAiChatModel` 会从 yaml 自动注入）

## 踩的坑

- **包名迁移**：源码从 `com.szb.quickstart` 批量改成 `com.caspian.quickstart`，别漏了 test 目录和资源文件路径（`ClassPathResource("images/cat.jpg")` 不涉及包名，所以没事）
- **Spring Boot 版本敏感**：`spring-boot-starter-parent 3.5.13` + LangChain4j 1.13.0 搭配 OK，换版本要注意 starter 自动装配的 bean 名会不会变
- **demo endpoint 的局限**：`http://langchain4j.dev/demo/openai/v1` 只适合跑通 Hello World，图片 URL 和 Base64 多模态在这个 endpoint 上不一定都能返回有效结果，真实多模态还是得用真实 API Key

## 结论

LangChain4j 最基础的"调一下模型"这件事，代码量很小（`builder().apiKey(...).build().chat(msg)`），真正开始变复杂是从 day04 AiService 开始把 Prompt/SystemMessage/Memory/Tools 串起来。
