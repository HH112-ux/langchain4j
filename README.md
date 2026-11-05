# LangChain4j 学习记录

这是我的 LangChain4j 学习记录：按天推进，一天一份笔记、一个对应模块，记录学习过程与练习代码。

## 目录说明

| 目录 | 说明 |
|------|------|
| `notes/` | 每天一篇学习笔记，day01–day04 已记录 |
| `quick-start/` | day02 的快速入门练习，文本、远程图片、本地图片三种调用方式 |
| `chat-dashscope/` | day03 的百炼平台接入练习，聊天、流式、绘图、语音、视频 |
| `ai-services/` | day04 的 AiServices 声明式接口练习，记忆、结构化输出、流式 |

根目录 `pom.xml` 是 Maven 父 POM（groupId `com.caspian`），三个模块由它聚合。

## 学习进度

| 日期 | 主题 | 对应目录 | 要点 |
|------|------|---------|------|
| 2026-11-02 | day01 什么是 LangChain4j | `notes/day01.md` | 核心抽象与它解决的问题，概念篇无代码 |
| 2026-11-02 | day02 快速入门 | `notes/day02.md`、`quick-start/` | 最小可运行调用，含多模态图片输入 |
| 2026-11-02 | day03 集成百炼平台 | `notes/day03.md`、`chat-dashscope/` | DashScope 多模态接入，异步任务与流式范式 |
| 2026-11-02 | day04 AiServices | `notes/day04.md`、`ai-services/` | 声明式接口装配记忆、结构化输出与流式回调 |

运行示例前需在环境变量中提供 `DASHSCOPE_API_KEY`，构建使用 JDK 17。
