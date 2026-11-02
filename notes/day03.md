# day03 学习笔记：接入百炼平台

## 为什么用百炼

- 国内直连，不用处理跨境网络；模型覆盖面广：Qwen 系列聊天、万相文生图、语音合成/识别、视频生成都能在一个平台上拿到 key。
- 计费与调用配额在同一个控制台里管理，切换模型不用再申请别家的账号。
- 对 Java 侧来说，LangChain4j 已经把这套服务封成了 starter，依赖、模型 bean、流式实现都现成，写业务只要注入接口。

## 依赖与配置

- 依赖：`langchain4j-community-dashscope-spring-boot-starter`。父 POM 已经通过 `langchain4j-community-bom` 管版本，子模块里不写 `<version>`，避免各模块版本漂移。
- 版本：中央仓库里 community 版是 `1.13.0-beta23` 这种带 beta 后缀的形式，少写后缀会解析不到。
- 配置项（`application.yaml`）：
  - `langchain4j.community.dashscope.chat-model.*`：非流式聊天模型，`qwen-plus`、`max-tokens=5000`、`temperature=0.7`
  - `langchain4j.community.dashscope.streaming-chat-model.*`：流式聊天模型，参数同上
  - `langchain4j.community.dashscope.image-model.*`：万相文生图，`wanx-v1`
  - `api-key` 统一写 `${DASHSCOPE_API_KEY:}`，由环境变量注入；仓库里不放真实 key
- 配置项名字必须对齐 starter 的自动装配前缀，少了 `community` 这一层，bean 就不会被创建（我一开始就踩过）。

## DashScope 和 OpenAI 协议的接入差别

- OpenAI 协议：一套 Chat Completions 格式走天下，文本/图片都塞进 `messages` 的 `content` 数组，模型能力差异主要体现在 `model` 字段。
- DashScope：聊天走的是自家 SDK 的消息结构；文生图、文生视频是**异步任务**——先提交拿 `taskId`，再轮询/等待结果；语音合成返回的是音频字节流，识别则要传本地文件。也就是说，同一把 key 下面并列着好几种调用范式，不能拿聊天接口的形状去套所有模型。
- 与 LangChain4j 的关系：community starter 底层就是阿里的 `dashscope-sdk-java`，所以像 `ImageSynthesis`、`SpeechSynthesizer` 这些官方类可以直接用，不需要我再单独引一份 SDK。
- 百炼也提供 OpenAI 兼容端点，可以用 `OpenAiChatModel` 指过去；但那只是协议兼容，绘图/语音/视频这些能力还是得走原生 SDK。

## 模块里的关键类

- `ChatDashscopeApplication`：启动类，本身没有业务代码，作用是让 starter 的自动装配生效。
- `QwenChatModel`：非流式聊天，`chat(String)` 直接返回完整文本；模块里既演示了 builder 手工构造，也演示了从容器注入。
- `QwenStreamingChatModel`：流式聊天，实现 `StreamingChatResponseHandler` 逐段收数据；`LambdaStreamingResponseHandler.onPartialResponseBlocking` 是更短的写法。
- `WanxImageModel`：LangChain4j 封装的文生图，`generate(prompt)` 返回 `Response<Image>`，取 `url()` 就能拿到图片地址。
- 官方 SDK 类：`ImageSynthesis`（同步出图，老一代模型）、`ImageGeneration`（异步任务 + `wait(taskId)`）、`SpeechSynthesizer`、`Recognition`、`VideoSynthesis`、`MultiModalConversation`。

## 怎么跑

```bash
export DASHSCOPE_API_KEY=你的key
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
mvn -pl chat-dashscope -am clean compile      # 只编译，不联网
mvn -pl chat-dashscope spring-boot:run        # 启动，端口 8081
mvn -pl chat-dashscope test -Dtest=DashScopeTest  # 真实调用，会产生费用
```

## 踩坑与结论

- **模型和接口要配对**：`ImageSynthesis` 只认 `wanx-v1` 这一类模型，换成 `wan2.7-image-pro` 会报错，那种模型得走 `ImageGeneration`。报错信息一般不会告诉你"模型不匹配"，只会给一个接口错误，容易误判成 key 或网络问题。
- **流式要等**：`chat(...)` 传 handler 是异步的，测试方法如果没有 `CountDownLatch` 之类的等待，会在回调打印之前就结束。
- **语音链路有先后**：识别用例读的是上一步写出的 `output.mp3`，单独跑识别会找不到文件；`language_hints` 只有 `paraformer-v2` / `paraformer-realtime-v2` 支持。
- **密钥不要进仓库**：`${DASHSCOPE_API_KEY:}` 留空时应用能启动但调用会失败，这是刻意的——本地填环境变量，仓库只留占位。
- 结论：LangChain4j 把"聊天"这件事抽象得很干净，但多模态/绘图/语音/视频这些能力在 DashScope 侧仍是各模型一套参数，封装只覆盖了常用路径，遇到新模型还是要翻官方 SDK 的类。

## 与 day02 的差别

day02 是走 OpenAI 兼容协议的 Hello World，一把 key 一个模型就能跑；今天换成原生 SDK 之后，同一把 key 能做的事多了，但每个能力的调用形状都不一样。下一步（day04）开始用 AiService 把这些散落的模型调用收敛成接口。
