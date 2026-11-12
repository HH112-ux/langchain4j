package com.caspian.rag.service;

import com.caspian.rag.config.RagProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * day08 RAG：摄入链路。
 * <p>
 * 课程里的四步在这里都有落点：加载 → 切分 → 向量化 → 写入向量库。
 * 对外提供两条等价路径，便于对照：
 * </p>
 * <ul>
 *   <li>{@link #ingestWithPipeline(List)}：用 {@link EmbeddingStoreIngestor} 一条龙完成，日常写法；</li>
 *   <li>{@link #ingestManually(List)}：手动分步，能看清每一步的输入输出。</li>
 * </ul>
 *
 * @author Caspian
 * @since 2026-11-12
 */
@Service
public class DocumentIngestService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestService.class);

    /**
     * Metadata 里记录来源文件的键名，检索命中后可以回溯到原文。
     */
    public static final String FILE_NAME_KEY = "file_name";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagProperties properties;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    /**
     * @param embeddingModel 向量模型
     * @param embeddingStore 向量库
     * @param properties     模块配置
     */
    public DocumentIngestService(EmbeddingModel embeddingModel,
                                 EmbeddingStore<TextSegment> embeddingStore,
                                 RagProperties properties) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.properties = properties;
    }

    /**
     * 应用启动时把 classpath 下的示例文档摄入内存向量库。
     * <p>
     * 演示项目选择启动即摄入；真实系统通常做成离线任务或增量同步。
     * </p>
     */
    @PostConstruct
    public void ingestOnStartup() {
        try {
            IngestReport report = ingestWithPipeline(loadDocuments());
            log.info("RAG 摄入完成：文档 {} 篇，片段 {} 条", report.documentCount(), report.segmentCount());
        } catch (IOException e) {
            throw new IllegalStateException("启动时加载文档失败", e);
        }
    }

    /**
     * 加载配置路径下的所有文档，并给每篇文档打上文件名。
     * <p>
     * 用通配资源加载而不是硬编码文件路径，打包成 jar 后同样可用。
     * </p>
     *
     * @return 已解析的文档列表，按文件名排序保证顺序稳定
     * @throws IOException 读取资源失败
     */
    public List<Document> loadDocuments() throws IOException {
        Resource[] resources = resourcePatternResolver.getResources(properties.ingest().docsPattern());
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
        TextDocumentParser parser = new TextDocumentParser();
        List<Document> documents = new java.util.ArrayList<>(resources.length);
        for (Resource resource : resources) {
            try (InputStream inputStream = resource.getInputStream()) {
                Document document = parser.parse(inputStream);
                document.metadata().put(FILE_NAME_KEY, String.valueOf(resource.getFilename()));
                documents.add(document);
            }
        }
        return documents;
    }

    /**
     * 用 {@link EmbeddingStoreIngestor} 完成切分、向量化、入库。
     * <p>
     * 这是课程里推荐的写法：分片器、向量模型、向量库一次装配，调用方只关心文档。
     * </p>
     *
     * @param documents 原始文档
     * @return 摄入结果摘要
     */
    public IngestReport ingestWithPipeline(List<Document> documents) {
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter())
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(documents);
        return new IngestReport(documents.size(), split(documents).size());
    }

    /**
     * 手动分步完成同样的四步，便于观察每一步。
     *
     * @param documents 原始文档
     * @return 摄入结果摘要
     */
    public IngestReport ingestManually(List<Document> documents) {
        List<TextSegment> segments = split(documents);
        embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);
        return new IngestReport(documents.size(), segments.size());
    }

    /**
     * 按配置的段长与重叠切分文档。
     * <p>
     * {@code chunkSize} 越大，单段信息越完整但检索越粗；{@code chunkOverlap} 用来避免
     * 一句话或一个条件被切在两段之间。本模块默认 300/50，属于中文问答文档的保守值。
     * </p>
     *
     * @param documents 原始文档
     * @return 切分后的文本段
     */
    public List<TextSegment> split(List<Document> documents) {
        DocumentSplitter splitter = splitter();
        return documents.stream()
                .flatMap(document -> splitter.split(document).stream())
                .toList();
    }

    /**
     * @return 按配置构造的递归分片器
     */
    private DocumentSplitter splitter() {
        return DocumentSplitters.recursive(
                properties.ingest().chunkSize(),
                properties.ingest().chunkOverlap());
    }

}
