# LangChain4j 学习记录

按天推进的学习笔记与配套练习代码。每完成一天的内容，追加一个 `notes/dayNN.md` 和相应代码模块。

## 当前进度

| Day | 主题 | 笔记 | 代码 |
|-----|------|------|------|
| 01 | 什么是 LangChain4j | ✅ `notes/day01.md` | —（概念篇） |
| 02 | 快速入门 | ✅ `notes/day02.md` | ✅ `quick-start/` |
| **03** | **集成百炼平台** | ✅ `notes/day03.md` | ✅ `chat-dashscope/` |
| 04 | AIService | 待续 | — |
| 05 | Memory | 待续 | — |
| 06 | Tool / Function Calling | 待续 | — |
| 07 | Spring Boot 集成 | 待续 | — |
| 08 | RAG | 待续 | — |

## 目录说明

```
langchain4j/
├── .gitignore
├── README.md
├── notes/                    # 每天一份学习笔记
│   ├── day01.md              # 概念速记
│   ├── day02.md              # quick-start 练习过程
│   └── day03.md              # 百炼平台接入
├── pom.xml                   # Maven 父 POM，groupId com.caspian
├── quick-start/              # day02 代码
│   ├── pom.xml
│   └── src/.../com/caspian/quickstart/
└── chat-dashscope/           # day03 代码
    ├── pom.xml
    └── src/.../com/caspian/chatdashscope/
```

## 运行

```bash
# 编译（不触发真实模型调用）
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
mvn -pl quick-start -am compile
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
mvn -pl chat-dashscope -am compile

# 启动 Spring Boot（quick-start 默认端口，chat-dashscope 为 8081）
mvn -pl quick-start spring-boot:run
mvn -pl chat-dashscope spring-boot:run

# 执行模型调用（需要真实 API Key）
mvn -pl quick-start test -Dtest=OpenAiTest
export DASHSCOPE_API_KEY=你的key
mvn -pl chat-dashscope test -Dtest=DashScopeTest
```

- Java 版本：pom 声明 17，本机用 Homebrew openjdk@17
- 模型配置走 `application.yaml`，`api-key` 用 `${ENV_VAR:}` 占位：`quick-start` 用 `langchain4j.open-ai.*`，`chat-dashscope` 用 `langchain4j.community.dashscope.*`
- 两个模块都不依赖外部服务；`chat-dashscope` 的绘图、语音、视频用例会真实请求百炼并产生费用

## 密钥与提交防护

仓库内不放任何真实凭据，配置一律写 `${DASHSCOPE_API_KEY:}` 这类环境变量占位符。

```bash
# 一次性安装：启用提交前的密钥扫描
cp scripts/hooks/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# 手动扫描（工作区 / 历史）
gitleaks detect --no-git --verbose
gitleaks detect --verbose
```

- 规则文件 `.gitleaks.toml`：继承 gitleaks 默认规则，仅放行 `${ENV_VAR:}` 形式的占位符
- 钩子行为：命中规则即阻止提交；本机未安装 gitleaks 时跳过扫描并给出提示，不阻断正常提交
- 仓库侧同时开启了 GitHub 的 secret scanning 与 push protection（免费个人仓库若不支持，见提交记录中的说明）
