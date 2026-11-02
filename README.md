# LangChain4j 学习记录

按天推进的学习笔记与配套练习代码。每完成一天的内容，追加一个 `notes/dayNN.md` 和相应代码模块。

## 当前进度

| Day | 主题 | 笔记 | 代码 |
|-----|------|------|------|
| 01 | 什么是 LangChain4j | ✅ `notes/day01.md` | —（概念篇） |
| **02** | **快速入门** | ✅ `notes/day02.md` | ✅ `quick-start/` |
| 03 | 集成百炼平台 | 待续 | — |
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
│   └── day02.md              # quick-start 练习过程
├── pom.xml                   # Maven 父 POM，groupId com.caspian
└── quick-start/              # day02 代码
    ├── pom.xml
    └── src/.../com/caspian/quickstart/
```

## 运行

```bash
# 编译（不触发真实模型调用）
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
mvn -pl quick-start -am compile

# 启动 Spring Boot
mvn -pl quick-start spring-boot:run

# 执行三种模型调用（需要真实 API Key）
mvn -pl quick-start test -Dtest=OpenAiTest
```

- Java 版本：pom 声明 17，本机用 Homebrew openjdk@17
- 模型配置走 `application.yaml`，`api-key` 用 `${ENV_VAR:}` 占位
- 没有外部服务依赖（Redis、向量库等）
