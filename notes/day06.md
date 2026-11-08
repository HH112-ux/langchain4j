# day06 学习笔记：工具调用

## 工具调用到底是谁在执行

- 模型不会执行 Java 代码。它只能在回复里表达意图："我想调用 `getWeather`，参数是 `city=北京`"，这个意图是 `AiMessage` 里的 `ToolExecutionRequest`。
- 真正干活的是应用：解析请求、调用本地方法、处理异常和权限，把结果包装成 `ToolExecutionResultMessage` 再发一次请求，模型基于结果生成最终回答。
- 所以一次工具调用至少是两轮模型请求。低级别代码里这个"两轮"是肉眼可见的；高级别 `@Tool` 只是把这两轮藏在框架里。
- 模型能看到的只有工具名、描述和参数 schema，看不到方法体。描述写得含糊，模型就会不调用、调错工具或编参数。

## 高级别：`@Tool` 怎么写

- 工具方法加 `@Tool("描述")`，参数加 `@P("描述")`。方法所在的类实例通过 `AiServices.builder(...).tools(weatherTools)` 注册。
- 参数默认都是必填。想让它可选，必须写 `@P(value = "描述", required = false)`；不写的话模型会硬编一个值出来。
- `@ToolMemoryId` 把会话 ID 传给工具方法，必须放在参数列表最后。它不参与模型生成，也不该出现在给模型的参数 schema 里。
- 返回类型决定回填格式：`String` 原样回填；`void` 回填固定文本 `"Success"`；record / POJO 转成 JSON 字符串。
- 避免返回巨大对象，回填内容会进下一轮请求的上下文，白烧 token。

实测断言（`ToolAssistantTest`）：

- `getWeather(String city, String unit, @ToolMemoryId Long memoryId)` 生成的 schema 里只有 `city` 和 `unit`，没有 `memoryId`；`required` 列表只有 `city`。
- 工具方法拿到的 `memoryId` 就是 `@MemoryId` 参数的值。
- `getAirQuality` 返回 record，回填文本是 `{"city":"北京","aqi":42,"level":"优"}` 这样的 JSON。
- `rememberPreference` 返回 `void`，回填文本精确等于 `"Success"`，同时方法体真的执行了（偏好列表里能读到值）。

## 用 `Result` 观察工具执行

- 接口方法返回 `Result<String>` 而不是 `String`，就能在调用后拿到：
  - `content()`：最终回答；
  - `toolExecutions()`：本次执行了哪些工具，每条有 `request()`（工具名、参数）和 `result()`（回填文本）。
- 排查"模型到底有没有调工具"时先看这里，比看最终回答可靠。
- `Result` 只适用于非流式；流式场景用 `TokenStream#onToolExecuted` 观察，前提是 builder 里挂了 `streamingChatModel`。

## 低级别：手动两轮链路

- 手动构造 `ToolSpecification`：`name`、`description`、`parameters(JsonObjectSchema...)`。
- 第一轮 `ChatRequest` 带上 `toolSpecifications(spec)`；如果 `aiMessage().hasToolExecutionRequests()` 为真，就自己解析 `toolExecutionRequests()`，自己执行本地方法。
- 第二轮请求的消息列表必须包含：原始用户消息、第一轮的 AI 消息（带工具意图）、每条 `ToolExecutionResultMessage`，同时继续带上工具规格。
- 少放第一轮 AI 消息或工具结果，模型会不知道工具已经执行过；只调一轮就结束，则拿到的只是"我要调工具"而不是最终回答。
- 实测（`ManualWeatherCallerTest`）：第二轮请求正好 3 条消息——用户、AI 工具意图、工具结果；工具结果里带着本地执行返回的城市天气。模型没请求工具时只请求一次，不会凭空多一轮。

## 动态工具：按请求决定暴露什么

- `AiServices.builder(...).toolProvider(provider)`：每次请求前调用 `ToolProvider.provideTools(ToolProviderRequest)`。
- 从 `request.userMessage().singleText()` 能拿到本轮用户输入，从 `request.chatMemoryId()` 能拿到会话 ID。
- 返回 `ToolProviderResult.builder().add(spec, (toolRequest, memoryId) -> "...").build()`；返回 `null` 表示本次不暴露任何工具。
- 适合按租户、权限或意图裁剪工具清单，避免几十个工具同时塞给模型造成选择困难。
- 实测（`DynamicToolProviderTest`）：问北京时第一轮请求里出现 `getBeijingWeather` 规格并成功执行；问上海时 `toolSpecifications` 为空，模型直接回答、没有工具执行记录。

## 两类兜底

- **幻觉工具名**：模型可能编造一个不存在的工具。默认策略直接抛 `RuntimeException`，消息里带工具名和 "no such tool exists"。
- 用 `hallucinatedToolNameStrategy(request -> ToolExecutionResultMessage.from(request, "不存在..."))` 回填一条纠正消息，让模型自我修正。
- 实测发现一个细节：被幻觉工具名触发的那次调用**也会出现在 `Result.toolExecutions()` 里**，结果文本来自兜底策略，而不是真实方法。所以不能把 `toolExecutions()` 当成"真实执行过的工具"计数，要结合结果内容判断。
- **循环上限**：模型可能"调工具 → 不满意 → 再调"。`maxSequentialToolsInvocations(n)` 限制连续工具调用轮数。
- 实测设 `n=1` 并让假模型每次都请求工具：框架执行完第一轮工具后，仍然发起了第二次模型推理，然后在下一轮检查处抛出 `RuntimeException`，消息是 "Something is wrong, exceeded 1 sequential tool invocations"。也就是说这个上限不是"最多只请求 n 次模型"，而是"最多处理 n 轮工具结果"。
- 兜底只是保险，不能替代清晰的工具描述。描述到位，模型本来就该选对。

## 踩坑与结论

- **`@ToolMemoryId` 放最后**不是风格问题：放前面会被模型当成普通参数，导致取值错误或参数校验失败；放最后框架才会跳过它且不写进 schema。
- **可选参数要显式 `required = false`**，光在 Java 里判空没用，模型根本不知道可以不传。
- **`String` 和 POJO 的回填格式不一样**：前者原样，后者转 JSON。想让模型稳定解析结构化数据，返回 record 比返回拼好的字符串更可靠。
- **工具重名会互相覆盖**，同一个名字只会留一个；多工具时用 `@Tool(name = "...")` 显式起唯一名字。
- **工具方法要自己兜异常**：方法抛异常时这次工具调用就失败了，模型拿到的是错误信息而不是业务结果；生产上还要给外部调用加超时。
- **本模块的离线测试覆盖**：高级别工具执行与 schema、返回类型、`@ToolMemoryId`、低级别两轮、动态 provider、幻觉兜底、循环上限，共 13 个用例（其中 1 个真实 API 用例默认跳过）。
- 真实 API 用例 `LiveToolCallTest` 由 `LIVE_API_TESTS=true` 门控，未设置时不联网、不计费，`mvn -o test` 保持全绿。

## 代码位置

- 模块：`chat-tools/`
- 工具定义：`tool/WeatherTools`、`tool/AirQuality`
- 高级别助手：`service/ToolAssistant`，装配在 `config/ToolConfig`
- 低级别链路：`manual/ManualWeatherCaller`
- 动态工具：`provider/BeijingWeatherToolProvider`、`service/DynamicToolAssistant`
- HTTP 验证入口：`controller/ToolController`（`/tools/chat`、`/tools/dynamic`，端口 8087）
- 离线测试：`ToolAssistantTest`、`ManualWeatherCallerTest`、`DynamicToolProviderTest`、`ToolSafetyTest`，测试支撑在 `support/ScriptedChatModel` 与 `support/ToolScripts`
