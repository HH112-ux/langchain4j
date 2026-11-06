# day05 学习笔记：ChatMemory

## 为什么需要 ChatMemory

- 模型本身没有状态：每次请求都是独立的，上一轮说过什么，这一轮完全不知道。
- 想实现"接着上一句聊"，就得把历史消息一起发过去。手写这段逻辑要做三件事：存历史、拼请求、控制长度——每个调用点重复一遍。
- ChatMemory 就是这三件事的封装：调用前把历史塞进请求，调用后把 AI 回复写回，超长时按策略裁剪。业务代码只管发这一句。

## 窗口策略怎么选

- `MessageWindowChatMemory` 按**条数**裁剪，`maxMessages(10)` 就是最多留 10 条。好处是可预期、开销固定；坏处是不管内容长短，一条"你好"和一条上千字的需求占用一样。
- `TokenWindowChatMemory` 按**token 估算**裁剪，`maxTokens(n, estimator)` 里要传一个估算器。更贴近模型的真实上下文上限，但估算器不准时会裁多或裁少。
- 注意系统消息也占一条（`@SystemMessage` 会被写进记忆），所以 10 条其实只够四轮多一点。
- 简单对话用条数窗口足够；接了长文档、大段代码再换 token 窗口。
- 估算器可以自己实现 `TokenCountEstimator`：我在测试里按"4 字符 ≈ 1 token"粗略折算，讲义没给具体规则，只用于演示裁剪行为。

## ChatMemoryProvider 与 @MemoryId 怎么做到多会话

- day04 的写法是 `.chatMemory(new MessageWindowChatMemory())`：一个助手实例只有一份记忆，所有用户共享，A 说的话 B 能看到。
- 改成 `.chatMemoryProvider(memoryId -> ...)` 后，框架每次调用都用方法上 `@MemoryId` 标注的参数值去问 provider 要记忆，不同 id 拿到不同实例。
- provider 里的关键一步是 `.chatMemoryStore(store)`：新实例构造时会按 id 从 store 把历史读回来。少了这一步，每次都是空的，隔离变成了失忆。
- 实测结论：同一个 id 的第二次请求带上了第一次的消息；不同 id 的历史互不可见，store 里也是两份独立的键。
- 另外确认了一个细节：记忆里存的是系统消息 + 用户消息 + AI 回复三条，不是两条。

## 持久化为什么要自定义 Store

- 默认的 `SingleSlotChatMemoryStore` 只是把消息挂在内存里，进程一停就没了；换 `InMemoryChatMemoryStore` 也只是多撑了一个进程的生命周期。
- 真正的持久化要自己实现 `ChatMemoryStore` 三个方法：`getMessages` / `updateMessages` / `deleteMessages`，key 就是 memoryId。
- Redis 版本的做法很直接：整段消息用 `ChatMessageSerializer.messagesToJson` 转成 JSON 存一个 String 键，读的时候 `ChatMessageDeserializer.messagesFromJson` 反序列化回来，再配一个 TTL 防止旧会话无限堆积。
- 光写不验等于没写，所以补了 `RedisChatMemoryStoreTest`：用 Docker 临时起 Redis 7.4（映射 6389），实测 4 个用例全绿——写入后读回系统/用户/AI 三种消息的类型与文本都不变；`updateMessages` 是覆盖而不是追加；窗口裁到 4 条后写进 Redis 的确实是 u3–u6；`deleteMessages` 后读不到消息且 `chat:memory:<id>` 键也消失。
- 没有 Redis 的机器不会因此变红：测试先用 500ms 超时探测端口，连不上就 assume-skip。把端口指向空的 6390 实测 4 个用例全部 skipped，构建仍是 BUILD SUCCESS；有 Redis 时用 `REDIS_PORT=6389 mvn -o test -pl chat-memory -Dtest=RedisChatMemoryStoreTest` 跑真实用例。
- 但 Redis 不是人人都有，所以做成配置开关：`chat-memory.store=memory` 走内存 Map，`=redis` 才装配 Redis 实现。两者接口一致，上层装配代码一行不改。
- 如果哪天换成 MySQL，同样只需要再写一个 `ChatMemoryStore` 实现，其余不动。

## 踩坑与结论

- **窗口裁剪是写回 store 的**：加满超过上限后，store 里存的是裁剪后的 4 条，不是全部 6 条。所以"持久化"保存的是窗口内的部分，不是完整聊天记录；要留全量得另建一张表。
- **provider 忘了带 store 就等于每次新建空记忆**，表现出来是"记住了又好像没记住"，排查时先看这一行。
- **@MemoryId 的类型要一致**：方法参数是 `Long`，provider 收到的 key 也是 `Long`，用 `1` 和 `1L` 会落成两个不同的键。
- **Redis 依赖在 classpath 不等于必须连 Redis**：Spring Boot 的连接工厂是懒加载的，只要不选 `redis` store 就不会去连。
- 结论：ChatMemory 解决"记不记得"，Provider + `@MemoryId` 解决"记得是谁的"，Store 解决"重启还在不在"。三层各管一件事，换实现时只动最下面那层。
