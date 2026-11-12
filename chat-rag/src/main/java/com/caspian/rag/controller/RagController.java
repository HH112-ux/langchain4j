package com.caspian.rag.controller;

import com.caspian.rag.dto.RagAnswer;
import com.caspian.rag.dto.RagChatRequest;
import com.caspian.rag.dto.SearchHit;
import com.caspian.rag.service.RagAssistant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * day08 RAG：检索与问答接口。
 * <p>
 * 一个接口面向最终用户（带检索上下文的回答），一个接口面向调试（只看命中片段），
 * 后者在调 chunkSize、maxResults、minScore 时最有用。
 * </p>
 *
 * @author Caspian
 * @since 2026-11-12
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagAssistant ragAssistant;

    /**
     * @param ragAssistant 检索增强问答服务
     */
    public RagController(RagAssistant ragAssistant) {
        this.ragAssistant = ragAssistant;
    }

    /**
     * 带检索上下文的问答。
     *
     * @param request 请求体，{@code question} 必填
     * @return 回答、引用来源以及是否命中资料
     */
    @PostMapping("/chat")
    public RagAnswer chat(@RequestBody RagChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question 不能为空");
        }
        return ragAssistant.chat(request.question());
    }

    /**
     * 只做检索，返回命中的片段，便于观察召回与打分。
     *
     * @param q 查询文本，必填
     * @return 命中片段列表，没有命中时返回空数组
     */
    @GetMapping("/search")
    public List<SearchHit> search(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q 不能为空");
        }
        return ragAssistant.search(q);
    }

}
