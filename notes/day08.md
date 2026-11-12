# day08 学习笔记：RAG 检索增强生成

## RAG 解决什么问题

- 模型知识有截止时间，训练之后的事情它不知道；私有知识（公司制度、内部文档）它也从来没见过。
- 更麻烦的是幻觉：不知道却硬编，语气还很笃定，用户很难分辨。
- 微调能补知识，但成本高、更新慢，改一条制度就要重训。RAG 换了个思路：不改模型，每次提问时先把相关资料检索出来塞进 prompt，让模型“看着材料回答”。
- 好处是知识可更新（换文档就行）、可追溯（回答能带出处）、可约束（资料里没有就明确说不知道）。
- 它不解决推理能力，只解决“模型手里有没有正确材料”。材料选错了，回答照样错，所以检索质量才是 RAG 的命门。

## 摄入四步各干什么

1. **加载**：把文档读成 `Document`。本项目用 `PathMatchingResourcePatternResolver` 按 `classpath:docs/*.md` 通配加载，打包成 jar 后路径依然有效；解析交给 `TextDocumentParser`，并给每篇文档打上 `file_name`，命中后能回溯来源。
2. **切分**：整篇文档太长，塞不进上下文也没法精确定位，切成 `TextSegment`。切分粒度直接决定“检索出来的东西是否刚好是你问的那一段”。
3. **向量化**：`EmbeddingModel` 把每段文本压成一个定长向量，语义相近的文本向量距离近。这是“按意思检索”而不是“按关键词匹配”的基础。
4. **入库**：向量 + 原文一起写进 `EmbeddingStore`。本项目用 `InMemoryEmbeddingStore`，重启即清空，够演示；生产换成 Milvus、PGVector 之类，只需替换这一个 bean。

两条路径都写了，互相印证：

- `ingestWithPipeline`：`EmbeddingStoreIngestor` 把分片器、向量模型、向量库一次装配，`ingest(documents)` 一条龙，这是日常写法；
- `ingestManually`：手动 `split` → `embedAll` → `store.addAll`，能看清每一步的输入输出，排查问题时更有用。

## 切分参数怎么影响召回

- `chunkSize` 是单段上限，默认取 **300**（字符）：太小会把一个完整规则切碎，字段缺一半；太大则一段里混着好几个主题，检索命中后噪声多，还挤占上下文和成本。
- `chunkOverlap` 是相邻段的重叠，默认取 **50**：防止“审批额度”和“审批人”被切在两段，检索只捞到半句。
- 两个值都是保守默认，注释里已注明依据：中文问答类文档、段落不长，300/50 能在“信息完整”和“定位精确”之间取平衡；文档换成合同、代码时应该重新调。
- 实测要注意一个行为：`DocumentSplitters.recursive(...)` **会把相邻的短段落合并到接近 chunkSize**，所以不是“按段数机械切”。测试里用 40/0 切三句短话期望得到 3 段，实际只得到 1 段，就是因为被合并了。
- 重叠也不是原样保留：递归分片器会按层级分摊重叠。测试里配置 20，实测有效重叠约 10（前后段最长公共部分）。所以断言不能写“后段以完整前段结尾”，只能算实际重合长度。

## 检索参数怎么调

`EmbeddingStoreContentRetriever` 只有两个关键旋钮，都从配置读取：

- `maxResults`（默认 **3**）：一次取回几段。太小可能漏掉正确片段；太大会把无关内容灌进 prompt，既增加成本又容易带偏模型。3 适合“一问一段”的知识库问答。
- `minScore`（默认 **0.5**）：相似度低于这个值直接丢弃，属于“宁缺毋滥”。调高更严格、更容易走“没有相关资料”分支；调低召回更多但噪声上升。不同 embedding 模型的分数分布不同，这个阈值必须用真实模型和真实文档重新标定，不能照搬。
- 二者配合的思路是：先用 `maxResults` 定上下文预算上限，再用 `minScore` 设相关性下限。调试用 `GET /rag/search?q=` 直接看命中片段和分数，比盲改参数高效得多。

## 高级检索补什么短板

- 向量召回是“粗排”：只看整体语义相似，容易漏掉关键词精确匹配，也容易在相似片段里排不准。
- 本轮做的是 **重排（ReRanking）**：`ReRankingContentAggregator` 在检索之后对候选片段重新打分、重新排序，补的是“精度”这块短板。
- 打分器 `KeywordOverlapScoringModel` 用字符二元组的 Jaccard 相似度代替模型重排，理由是离线可跑、结果可解释；接口仍是标准的 `ScoringModel.scoreAll(segments, query)`，生产换成 Jina / Cohere 的交叉编码器只改这一个类。
- 没做的是**查询改写 / 多查询（ExpandingQueryTransformer、多查询扩展）**：它要多调一次甚至多次 LLM，成本和延迟都上去，且在假模型下无法体现真实效果。本轮选择先把“粗排 + 精排”跑通，改写留到有真实模型和真实语料时再验证，避免为了凑条目写一段没被验证过的代码。

## 踩坑与结论

- **任意阈值不可靠**：`minScore` 的绝对值和 embedding 模型强相关。离线哈希向量和 DashScope 向量的分数分布完全不同，所以测试只用 0 和 0.99 这样的两端值验证“参数生效”，不拿具体分值当断言；生产阈值要重新标定。
- **短段会被合并**：`DocumentSplitters.recursive` 不是机械按段切，写切分测试前先确认实际产物，否则断言必挂。
- **有效重叠 ≠ 配置重叠**：递归分片按层级分摊，配置 20 实测约 10，测试改为计算实际重合长度并断言下限。
- **“无命中别硬编”要靠代码兜底**：光靠 system message 约束不够稳。`RagAssistant.chat` 先检索一次，命中为空就直接返回固定话术、根本不调用模型，这才是硬闸。
- **代价是重复检索**：为了预判无命中，检索做了两次（一次自己判断、一次 `AiServices` 增强器内部再检）。片段少时可以接受，量大时应改为自定义 `RetrievalAugmentor` 或缓存结果。
- **流式/记忆没有接**：day08 只做“检索 → 生成”的主线，未叠加 day05 的多会话记忆和 day07 的 SSE；叠上去时要注意增强器每次调用都会重新检索，历史问题和当前问题会被拼成新的查询。
- **结论**：RAG 的工程质量几乎全在“切分 + 检索参数 + 兜底策略”上。模型只是最后一环，前面选错材料，后面再强的模型也只能编得更像。

## 代码位置

- 模块：`chat-rag/`（端口 8089）
- 示例文档：`src/main/resources/docs/company-handbook.md`（自写的员工手册节选，含差旅、年假、VPN、设备四节）
- 摄入：`service/DocumentIngestService`（`loadDocuments` / `ingestWithPipeline` / `ingestManually` / `split`）
- 装配：`config/RagConfig`（真实模型、向量库、检索器、重排、增强器）、`config/RagProperties`
- 重排：`rerank/KeywordOverlapScoringModel`
- 问答：`service/RagAssistant`（`chat` / `search` / `retrieve`）、`service/KnowledgeAssistant`
- HTTP 入口：`controller/RagController`（`POST /rag/chat`、`GET /rag/search?q=`）
- 离线测试：`DocumentIngestServiceTest`、`RetrievalTest`、`RagAssistantTest`、`RagControllerTest`，支撑在 `support/`（哈希 embedding + 录制式聊天模型）
- 真实调用：`LiveRagTest`，由 `LIVE_API_TESTS=true` 门控
