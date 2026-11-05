# day04 学习笔记：AiServices 声明式接口

## 这一节解决什么问题

- 前几天是"手动挡"：自己 new 模型、自己拼消息、自己从 `ChatResponse` 里抠文本。能力越多，业务代码里模型调用越散。
- AiServices 把调用收回成"接口契约"：我只写一个没有实现的接口，框架在运行时生成代理，负责消息组装、模型调用、返回值转换。
- 好处很直接：业务侧只看到 `Assistant.chat("...")`，换模型、加记忆、加流式都只改装配处，不动调用点。

## 声明式接口怎么写

- 接口里的一个方法 = 一次对话。方法参数默认被当作一条用户消息。
- `@SystemMessage` 定角色和约束；`@UserMessage` 写用户消息模板，`{{topic}}` 配合 `@V("topic")` 把参数填进去。
- 装配三件套：`AiServices.builder(Assistant.class)` + `.chatModel(...)` +（可选）`.streamingChatModel(...)`、`.chatMemory(...)`，`build()` 出来的对象直接当 Spring Bean 用。
- 接口方法不能有实现、也不能是 default 方法，否则框架的代理生成逻辑对不上。
- 返回类型不止 String：record/POJO 可以拿结构化结果，`TokenStream` 拿流式，`Result<T>` 还能带 token 用量。

## 记忆怎么生效

- 挂上 `MessageWindowChatMemory.withMaxMessages(10)` 后，每次调用前框架会把"历史消息 + 本次用户消息"整包发给模型，调用结束再把 AI 回复写回记忆。第二轮能接着上一轮聊，就是因为历史被塞进了请求里。
- 窗口按消息条数算（user 和 ai 各算一条），系统消息也占一格，所以 10 条大约只够四轮多一点；调大窗口并不能解决"记不住很久以前"的问题，只会让请求变长。
- 一个单例 memory 是全局共享的。多用户/多会话要配 `ChatMemoryProvider` + `@MemoryId` 按会话隔离。
- 默认记忆在堆内存里，重启即失；要跨重启得自己实现持久化的 `ChatMemoryStore`（day05 会碰 Redis）。

## 结构化输出怎么做到

- 方法直接返回自定义 `record CourseOutline(String title, List<String> chapters)`，框架按这个类型生成 JSON 结构说明，追加到提示词里（模型声明了 JSON Schema 能力时改走 response_format）。
- 模型回复后框架用 Jackson 反序列化，字段名要和 record 组件名对上。
- 模型必须只吐 JSON：多一句"好的，这是您要的大纲"或者包裹 ```json 代码块，都会解析失败。这点在真实模型上比假模型更容易翻车。

## 流式的回调时机

- 流式方法的返回类型是 `TokenStream`，链式注册 `onPartialResponse`（增量片段）、`onCompleteResponse`（完整响应）、`onError`，最后 `start()` 才真正发请求。
- `start()` 是异步的，主线程立刻返回；测试里必须用 `CountDownLatch` 等完成回调，否则断言时片段还没收齐。
- `onPartialResponse` 适合边生成边渲染；`onCompleteResponse` 才是拿 token 用量、finishReason 的地方。
- 前提是 builder 里配了 `streamingChatModel`，只配 `chatModel` 时流式方法会在运行时报错。
- `onError` 一定要写，否则网络中断时既没有结果也没有异常，表现成"卡住"。

## 踩坑与结论

- 1.x 把 `ChatLanguageModel` 改名为 `ChatModel`，按旧文档写会直接编译不过。
- 同一个类不能既 `implements ChatModel` 又 `implements StreamingChatModel`：两个接口都声明了 `supportedCapabilities()`、`provider()`、`listeners()` 等默认方法，编译器会报"继承了不相关的默认值"。拆成两个假模型类才编译通过。
- 假模型是离线测试的关键：把每次收到的 `ChatRequest` 存下来，就能断言系统消息是否注入、模板变量是否替换、历史条数是否正确，全程不联网也不需要 key。
- 结论：AiServices 把"调模型"提升为"调接口"，记忆、结构化输出、流式不过是同一套装配的不同开关。记住 builder 的三个挂载点（`chatModel` / `streamingChatModel` / `chatMemory`），后面的 tool 和 RAG 也只是继续往里加组件。
