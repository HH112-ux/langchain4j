# 八天 LangChain4j 总结（写给三周后的自己）

## 一句话结论

LangChain4j 的价值不在"能调模型"——那件事一个 HTTP client 就够了。它的价值是把**记忆、工具、检索**
这三类"模型本身没有的能力"做成可替换的组件，让它们能在同一套 `AiServices` 装配里叠加。

值得引入：需要多轮上下文、需要调内部系统、需要基于私有文档回答。
不必引入：一次性文本处理、单轮问答、纯 prompt 调优——那些用裸 SDK 更省事，多一层抽象只是负担。

## 八天的主线演进

- **day02 调用模型**
  - 做了什么：`ChatModel` 发消息拿回复；多模态只是把 `ImageContent` 塞进 `UserMessage` 的 content 列表。
  - 补上：把"调模型"变成一行 builder 调用。
  - 暴露：模型是无状态函数，上一轮说过什么它完全不知道——这是后面一切复杂性的源头。

- **day03 接入百炼**
  - 做了什么：从 OpenAI 兼容协议换到 DashScope 原生 SDK，聊天、流式、文生图、语音、视频并排跑。
  - 补上：一个 provider 下的多模态能力，且 starter 已经封装好常用路径。
  - 暴露：同一把 key 下调用范式并不统一——绘图和视频是异步任务，语音是字节流，封装只覆盖常用模型。

- **day04 声明式 AiServices**
  - 做了什么：把散落的 `builder().chat()` 收敛成接口；方法即一次对话，`@SystemMessage`、`TokenStream`、record 返回值都是装配开关。
  - 补上：day02/03 每个调用点重复拼消息、抠 `ChatResponse` 的重复。
  - 暴露：记忆是单个实例、全局共享，多用户会串台；结构化输出要求模型只吐 JSON，真实模型容易多话。

- **day05 记忆**
  - 做了什么：`ChatMemory` 管裁剪，`ChatMemoryProvider` + `@MemoryId` 管"是谁的"，`ChatMemoryStore` 管"重启还在不在"。
  - 补上：day04 单例 memory 的串台与失忆。
  - 暴露：窗口按消息条数算而不是按轮数（系统消息也占一格）；窗口裁剪会写回 store，持久化的是片段不是全量。

- **day06 工具调用**
  - 做了什么：模型只在 `AiMessage` 里表达"我想调 `getWeather(city=北京)`"，应用执行后再发一轮；高级别 `@Tool`、低级别 `ToolSpecification`、动态 `ToolProvider` 都跑了一遍。
  - 补上：day04/05"模型只能说不能做"。
  - 暴露：工具描述含糊就会调错或编参数；幻觉工具名、无限循环、可选参数都要显式兜底。

- **day07 Spring Boot 集成**
  - 做了什么：模型变成配置驱动的 bean，助手由 `@AiService` 扫描生成，`EXPLICIT` 按名字引用，SSE 走 `Flux<ServerSentEvent>`。
  - 补上：day04 手工装配散在 Java 里、换环境要改代码。
  - 暴露：`api-key` 空串也算属性存在，bean 照建直到调用才失败；流式要"三件套"同时到位。

- **day08 RAG**
  - 做了什么：文档加载 → 切分 → 向量化 → 入库，检索结果经重排后注入 prompt；无命中直接返回固定话术。
  - 补上：day06 工具只能查结构化接口的盲区——非结构化文档也能成为知识来源。
  - 暴露：切分粒度与检索阈值决定成败，模型只是最后一环；`minScore` 必须用真实 embedding 标定。

一句话串起来：**调用 → 多模态 → 声明式 → 有记忆 → 能动手 → 交给容器 → 有外部知识**。
每一步都在补上一步暴露的短板，而不是往上叠功能。

## 两条线索：能力在加，装配在减

- **能力线**（模型本身不会的事）：记忆、工具、检索。三者互相独立，可以任意组合，
  这也是 LangChain4j 比"一个黑盒 SDK"更值得用的地方。
- **装配线**（怎么把这些能力挂上去）：day02 手写 builder → day04 `AiServices.builder` → day07 容器与 `@AiService`。
  同一条链路被抽象了三层，越往后业务代码越短，配置与启动期校验承担得越多。
- 两条线的交叉点就是 `AiServices`：能力从它的 builder 挂进来，装配方式由谁 new 它决定。
  面试或复习时，只要能把"某个能力从哪个挂载点进来"说清，就不会混。

## 核心抽象速查

| 抽象 | 是什么 | 什么时候用 | 出现 |
|------|--------|-----------|------|
| `ChatModel` / `StreamingChatModel` | 模型调用的两种形态：一次性返回 / 逐 token 回调。1.x 里 `ChatLanguageModel` 已更名为 `ChatModel` | 普通问答用前者；要打字机效果用后者，且需同时挂 `streamingChatModel` | day02 / day04 |
| `AiServices` | 运行时为接口生成代理，负责消息组装、模型调用、返回值转换 | 只要不想在业务代码里手拼 `ChatRequest` 就用它 | day04 |
| `ChatMemory` | 单会话的消息窗口，负责"调用前塞历史、调用后写回、超长裁剪" | 单会话助手 | day04 |
| `ChatMemoryProvider` | 按 memoryId 现场产出记忆实例 | 多用户/多会话，必须配 `@MemoryId` | day05 |
| `ChatMemoryStore` | 记忆的持久化后端（内存 / Redis / 数据库） | 需要跨重启保留历史时 | day05 |
| `@Tool` / `ToolSpecification` / `ToolProvider` | 声明式工具 / 低级别工具规格 / 按请求动态暴露工具 | 需要模型触发本地方法；工具太多会干扰选择时上 provider | day06 |
| `ContentRetriever` | 把查询变成候选片段（通常是向量检索） | 需要"按意思找材料"时 | day08 |
| `RetrievalAugmentor` | 检索结果 → 注入 prompt 的编排层，可挂聚合与重排 | 接 `AiServices` 的 RAG 入口 | day08 |
| `EmbeddingModel` / `EmbeddingStore` | 文本 → 向量 / 向量与原文的存储与相似度检索 | 摄入与检索的两端；换向量库只换 store | day08 |
| `Result<T>` | 回答 + 来源 + token 用量 + 工具执行记录 | 需要观测"到底检索了什么、调了什么工具"时 | day06 / day08 |

## 八个模块对照表

| 模块 | day | 关键类 | 一句话作用 |
|------|-----|--------|-----------|
| `quick-start/` | day02 | `QuickStartApplication`、`OpenAiTest` | OpenAI 兼容端点跑通文本、远程图片、本地 Base64 三种输入 |
| `chat-dashscope/` | day03 | `ChatDashscopeApplication`、`DashScopeTest` | 同一把百炼 key 下的聊天、流式、绘图、语音、视频 |
| `ai-services/` | day04 | `assistant/Assistant`、`CourseOutline`、`config/AiServicesConfig` | 声明式接口 + 窗口记忆 + record 结构化输出 + `TokenStream` |
| `chat-memory/` | day05 | `service/ChatPersistenceAssistant`、`store/RedisChatMemoryStore`、`config/MemoryConfig` | provider 按 `@MemoryId` 隔离，store 可切内存 / Redis |
| `chat-tools/` | day06 | `tool/WeatherTools`、`manual/ManualWeatherCaller`、`provider/BeijingWeatherToolProvider` | `@Tool` 高级别、手动两轮低级别、动态工具与两类兜底 |
| `spring-boot-app/` | day07 | `service/AssistantService`、`config/ChatMemoryConfig`、`controller/ChatController` | starter 配置驱动 + `@AiService` EXPLICIT + SSE 流式 |
| `chat-rag/` | day08 | `service/DocumentIngestService`、`service/RagAssistant`、`config/RagConfig` | 摄入四步、向量检索、重排、无命中兜底与两个 HTTP 接口 |

## 踩过的坑（会再犯的才留）

- **系统消息也占记忆窗口一格**：`maxMessages(10)` 只够四轮多；窗口按消息条数算，不是按对话轮数。
- **provider 忘了带 store 就是每次新建空记忆**：表现是"记住了又好像没记住"，排查先看 `.chatMemoryStore(...)` 那一行。
- **窗口裁剪是写回 store 的**：持久化保存的是裁剪后的片段，不是完整聊天记录；要留全量得另建存储。
- **`api-key` 配了空串也算属性存在**：`@ConditionalOnProperty` 仍会创建模型 bean，直到真正调用才报鉴权失败；离线测试干脆不写这段配置。
- **`Flux` 流式是三件套**：`streamingChatModel` 配置项、容器里的流式模型 bean、`langchain4j-reactor` 依赖，缺一个都在运行时才报错。
- **`@ToolMemoryId` 必须放参数列表最后**：放前面会被模型当成普通参数写进 schema，取值错误；放最后框架才会跳过它。
- **工具可选参数必须显式 `required = false`**：只在 Java 里判空没用，模型不知道可以不传，会硬编一个值。
- **幻觉工具名也会出现在 `Result.toolExecutions()`**：结果文本来自兜底策略而非真实方法，不能把它当"真实执行计数"。
- **结构化输出要求模型只吐 JSON**：多一句"好的，这是您要的"或包一层代码块就会反序列化失败，真实模型比假模型更容易翻车。
- **`DocumentSplitters.recursive` 会合并短段，且有效重叠 ≠ 配置重叠**：配置 20 实测约 10；而 `minScore` 的绝对值强依赖 embedding 模型，必须用真实模型和真实语料重新标定。

## 尚未验证的部分（以及怎么补）

- **真实 DashScope 调用**：本机没有 key，八天都只做了离线验证。
  补法：`export DASHSCOPE_API_KEY=...` + `LIVE_API_TESTS=true`，逐模块跑 `-Dtest=Live*`；
  先看 day03 的多模态，异步任务最容易暴露模型与接口不匹配。
- **Redis 持久化记忆**：只用一个 Docker 临时 Redis 验证过 `RedisChatMemoryStore` 单测，
  没在真实应用链路跑过"两轮对话 → 重启 → 续聊"。
  补法：`docker run -p 6389:6379 redis`，启动时设 `chat-memory.store=redis`，重启后再问上一轮的内容。
- **真实向量库**：现在用 `InMemoryEmbeddingStore`，重启即清空，也没有标量和元数据过滤。
  补法：把 `embeddingStore` 这一个 bean 换成 PGVector / Milvus 实现，重跑 `RetrievalTest` 对比召回。
- **查询改写 / 多查询**：day08 只做了重排，没做 `ExpandingQueryTransformer`。
  补法：接真实模型后加改写分支，用同一批问题对比改写前后的 recall@3，没提升就别留。
- **真实多模态（图片/语音/视频）**：day02 的 demo endpoint 对图片 Base64 不保证有结果，
  day03 的语音链路依赖上一步产物，都没有在真实 key 下端到端验证过。

## 如果继续往下学

1. **接真实向量库 + 元数据过滤**：最该先做。`InMemoryEmbeddingStore` 是玩具件，
   而"按部门/版本/权限过滤"决定了 RAG 能不能进生产，比换更花哨的检索算法实在。
2. **把记忆和 RAG 叠加**：真实用户不会只问一句，"那这个呢"这种追问必须靠历史补全指代，
   否则检索直接跑偏。这是 day05 与 day08 的交汇处，也是最有价值的组合。
3. **建一套检索评估**：准备"问题 → 正确片段"对照集，算 recall@k。
   没有这个，调 `chunkSize` 和 `minScore` 只能凭感觉，改完也不知道是变好还是变坏。
4. **换真实 rerank 模型**：把 `KeywordOverlapScoringModel` 换成 Jina / Cohere 的交叉编码器，
   接口不变、只换一个类，是成本最低的效果验证。
5. **加引用与流式前端**：用户需要看到"答案依据哪一段"，`Result.sources()` 已经有了，
   缺的是把它渲染出来；流式 + 可点击引用才是可交付的形态。

## 最后一句

这八天真正学到的不是 API 用法，而是**每个能力各自的失效模式**——
记忆会串台、工具会幻觉、检索会跑偏。能把失效模式讲清楚，比能默写 builder 更有用。
