# day01 学习笔记：什么是 LangChain4j

## 它解决什么问题

给 Java 开发者一套统一、模块化的 API，把大模型（LLM）、向量存储、工具调用、对话记忆这几块粘起来，不用每个 AI 项目都从零写一遍 HTTP client + 相似度检索 + prompt 拼装。

## 和 Spring AI 的差别

两个都做 Java AI 集成，但取向不同：

| 维度 | LangChain4j | Spring AI |
|------|------------|-----------|
| 风格 | 底层、模块化、自由拼装 | 注解驱动、起步快 |
| 灵活度 | 高——Tool/RAG/Memory 随意组合 | 中等——默认流程写死 |
| 生态 | 模型多（OpenAI、DashScope、Ollama…）、向量库多（Chroma、Milvus、PGVector…） | 绑定 Spring 体系 |

不是互斥：LangChain4j 有 `langchain4j-spring-boot-starter`，可以直接在 Spring Boot 里用。

## 核心抽象（按后续几天要写代码的先后记）

| 抽象 | 它是什么 | 第一次出现在 |
|------|---------|-------------|
| `ChatModel` / `ChatLanguageModel` | 模型调用的统一接口，换模型不换业务代码 | day02 quick-start |
| `EmbeddingModel` | 把文本变成语义向量 | day08 RAG |
| `ChatMemory` / `ChatMemoryStore` | 多轮对话的上下文存储（内存或 Redis 持久化） | day05 |
| `AiServices` / `@AiService` | 把接口 + Prompt + Memory + Tools 串成一个 AI Service | day04 |
| `Tool` | 给模型挂的外部能力（查天气、调数据库等） | day06 |
| `RetrievalAugmentor` | 检索增强（RAG 的核心）：把向量检索的结果拼进 Prompt | day08 |

## 几个容易混淆的概念

- **Prompt** vs **SystemMessage**：Prompt 是统称；SystemMessage 是给模型的人设/指令部分，和 UserMessage、AssistantMessage 并列
- **Token**：模型分词的最小单位，计费按 token 数；估算器叫 `TokenCountEstimator`
- **Builder 模式**：LangChain4j 的 API 几乎全是 builder 风格（`OpenAiChatModel.builder().apiKey(...).build()`），写的时候注意别漏 `.build()`

## 一句话总结

LangChain4j 把"让 Java 调大模型"这件事拆成了可替换、可组合的几块，而不是一个黑盒子 SDK。后面七天的代码就是把这几块一块块拼起来看怎么工作。
