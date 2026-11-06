package com.caspian.quickstart;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;


/**
 * day02 快速入门：OpenAI Chat Model 三种调用方式示例。
 * <ul>
 *     <li>{@link #textTest()} — 纯文本问答，最短链路</li>
 *     <li>{@link #imageTest()} — 远程图片 URL 作为 ImageContent</li>
 *     <li>{@link #imageTest2()} — 本地 cat.jpg 转 Base64 的多模态输入</li>
 * </ul>
 * 默认跳过；需要真实调用时设置 {@code LIVE_API_TESTS=true} 并确保端点可用。
 *
 * @author caspian
 * @since 2026-11-02
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "LIVE_API_TESTS", matches = "true")
public class OpenAiTest {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;


    @Test
    void textTest(){
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder().apiKey(apiKey).modelName(modelName).baseUrl(baseUrl).build();
        String answer = openAiChatModel.chat("你是谁");
        System.out.println(answer); // Hello World
    }

    @Test
    void imageTest(){
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder().apiKey(apiKey).modelName(modelName).baseUrl(baseUrl).build();
        UserMessage message = UserMessage.from(TextContent.from("描述这个图片"),
                ImageContent.from("https://images.pexels.com/photos/20424752/pexels-photo-20424752.jpeg"));
        ChatResponse response = openAiChatModel.chat(message);
        System.out.println(response.aiMessage().text());
    }

    @Test
    void imageTest2() throws IOException {
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder().apiKey(apiKey).modelName(modelName).baseUrl(baseUrl).build();
        ClassPathResource resource = new ClassPathResource("images/cat.jpg");
        InputStream inputStream = resource.getInputStream();
        byte[] imageBytes = FileCopyUtils.copyToByteArray(inputStream);
        String base64Data = Base64.getEncoder().encodeToString(imageBytes);
        ImageContent imageContent = ImageContent.from(base64Data, MimeTypeUtils.IMAGE_JPEG_VALUE);
        UserMessage userMessage = UserMessage.from(imageContent, TextContent.from("描述这个图片"));
        ChatResponse response = openAiChatModel.chat(userMessage);
        System.out.println(response.aiMessage().text());
    }



}
