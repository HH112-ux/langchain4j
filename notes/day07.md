# day07 学习笔记：Spring Boot 集成 LangChain4j

## Spring Boot 集成解决了什么

- day04 里模型、记忆、助手全靠手写 `@Configuration` 装配；每换一个模型或改一个参数都要改 Java 代码、重新编译。
- day07 把这三件事交给容器：模型是配置驱动的 bean，助手是扫描出来的代理，记忆是容器里的两个 bean。业务代码只注入接口，不再出现 `AiServices.builder(...)`。
- 好处很直接：环境差异收敛到 `application.yaml`，key 走环境变量占位；模型实例复用，不用每次请求新建；启动期就能发现配置缺失，而不是运行到一半才报错。

## starter 自动配置怎么生效

- 两个依赖分工明确：
  - `langchain4j-community-dashscope-spring-boot-starter` 负责模型 bean；
  - `langchain4j-spring-boot-starter` 负责 `@AiService` 的扫描与代理生成。
- 模型 bean 的创建条件绑定在配置项上，而不是无条件创建：
  - 存在 `langchain4j.community.dashscope.chat-model.api-key` 才创建 `qwenChatModel`；
  - 存在 `langchain4j.community.dashscope.streaming-chat-model.api-key` 才创建 `qwenStreamingChatModel`。
  这是我在 starter 的自动配置类里确认过的行为，不是猜的。
- 模型名、温度、max-tokens、base-url 都在对应节点的属性里配。DashScope 原生接口的 base-url 是 `/api/v1`；不写就用 SDK 默认值，这里显式写出来便于切换环境。
- `qwenChatModel` / `qwenStreamingChatModel` 这两个 bean 名字很关键，下面的 EXPLICIT 装配直接按名字引用。

## `@AiService` 与 wiringMode 怎么选

- 接口加 `@AiService` 即可，starter 启动时扫描并注册代理 bean，`@Resource` / 构造器注入都能拿到。
- 两种装配模式：
  - `AUTOMATIC`：按类型在容器里找唯一 bean，适合只有一个模型的应用；
  - `EXPLICIT`：按名字引用 `chatModel`、`streamingChatModel`、`chatMemoryProvider`，配置写错时启动即失败，不会悄悄用错模型。
  本项目用 EXPLICIT，因为同时存在普通模型和流式模型，按名字最不容易误解。
- 方法级注解沿用 day04：`@SystemMessage` 定人设，`@UserMessage` 标用户输入，`@MemoryId` 标会话标识。
- 返回 `Flux<String>` 的方法需要三样东西同时到位：`streamingChatModel` 属性、流式模型 bean、以及 `langchain4j-reactor` 依赖（它通过 SPI 把内部的 `TokenStream` 适配成响应式流）。

## 流式接口在 Spring 里怎么落地

- 父 POM 已经是 WebFlux，所以 Controller 直接返回 `Flux<ServerSentEvent<String>>`，`produces = text/event-stream`，框架负责把每个事件写成 SSE 帧并保持连接。
- 每个 token 映射成一个事件；流结束后追加一个 `event: done`、`data: [DONE]` 的哨兵事件。客户端看到它就主动收尾，而不是依赖连接断开。
- 异常用 `onErrorResume` 转成 `event: error` 事件，再让流正常完成。否则异常会直接掐断连接，前端只看到半截回答。
- 空消息在进模型之前就拦掉，同样推一个 error 事件，避免无意义的计费调用。
- 前端为什么不用 `EventSource`：它只支持 GET、不能带请求体。案例里用 `fetch` 发 POST，再手工按 `\n\n` 切分事件流。
- 记忆在流式下同样生效，因为 `@MemoryId` 与普通方法共用同一个 provider。

## 和 day04 手工装配的对比

| 维度 | day04 | day07 |
|------|-------|-------|
| 模型创建 | `QwenChatModel.builder()...build()` 写在 Java 里 | starter 按 `application.yaml` 创建 |
| 参数调整 | 改代码重编译 | 改配置重启 |
| 助手创建 | `AiServices.builder(...).build()` | `@AiService` 扫描生成代理 |
| 记忆 | 单个 `ChatMemory` 或手写 provider | 容器里的 `ChatMemoryProvider` + `ChatMemoryStore` |
| 依赖获取 | 手动 `new` 或从配置类取 | 直接注入接口 |
| 适合场景 | 理解底层链路 | 真实应用的默认写法 |

## 踩坑与结论

- **Provider 必须配共享的 store**：`ChatMemoryProvider` 每次请求都会新建 `MessageWindowChatMemory`，如果每个实例自带独立存储，历史在请求结束时就没了。day05 踩过一次，day07 在容器里同样要显式声明 `ChatMemoryStore`。
- **api-key 属性存在但为空也会创建 bean**：`${DASHSCOPE_API_KEY:}` 在未设置时解析为空串，`@ConditionalOnProperty` 仍认为属性存在，于是模型 bean 照常创建，直到真正调用才报鉴权失败。离线测试因此不写这段配置，让自动配置整体不生效。
- **测试上下文会缓存**：同一份上下文被多个用例复用，假模型里累积的请求数会串；断言前先取基线，别写死索引。
- **SSE 的结束要有显式标记**：`EventSource` 在连接关闭后会自动重连，没有 done 事件会让前端反复请求。
- **1.x 的命名**：`ChatLanguageModel` 已更名为 `ChatModel`，流式侧是 `StreamingChatModel`，与本项目其他模块保持一致。
- **结论**：day04 学的是"装配原理"，day07 学的是"把原理交给框架"。生产中优先用 starter + EXPLICIT，把不确定性留给启动期。

## 代码位置

- 模块：`spring-boot-app/`（端口 8088）
- 入口与配置：`SpringBootAppApplication`、`application.yaml`
- 声明式服务：`service/AssistantService`（`@AiService` + `@MemoryId` + `Flux`）
- 记忆装配：`config/ChatMemoryConfig`
- HTTP 入口：`controller/ChatController`（`GET /springboot/chat`、`POST /springboot/chat/sse`）
- 离线测试：`AssistantServiceTest`、`ChatControllerSseTest`，测试支撑在 `support/`
- 真实调用：`LiveApiTest`，由 `LIVE_API_TESTS=true` 门控，配合 `live` profile
