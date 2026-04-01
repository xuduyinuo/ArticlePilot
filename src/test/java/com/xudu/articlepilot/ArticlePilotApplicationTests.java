package com.xudu.articlepilot;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

@SpringBootTest
@ActiveProfiles("local")
class ArticlePilotApplicationTests {

    @Test
    void contextLoads() {
    }



    @Resource
    private DashScopeChatModel chatModel;

    @Test
    public void testChat() {
        // 同步调用
        String response = chatModel.call("你好，请告诉我你的详细模型版本名称？");
        System.out.println(response);

        // 流式调用
        // Flux<ChatResponse> stream = chatModel.stream(
        //         new Prompt("用一句话介绍 Spring AI")
        // );
        // stream.subscribe(chunk ->
        //         System.out.print(chunk.getResult().getOutput().getText())
        // );
    }



}
