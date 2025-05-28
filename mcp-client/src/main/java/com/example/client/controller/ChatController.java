package com.example.client.controller;

import com.example.client.util.MarkdownUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天控制器，处理AI聊天请求
 */
@RestController
public class ChatController {

    private ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个MCP小助手，可根据需求调用不同的MCP工具，优化回答效果")
                // 注册工具方法
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    /**
     * 处理聊天请求，使用AI和MCP工具进行响应
     */
    @RequestMapping("/api/chat")
    public String chat(@RequestParam String message) {
        // 使用API调用聊天
        String content = chatClient.prompt()
                .user(message)
                .call()
                .content();

        System.out.println(">>> 问题: " + message);
        System.out.println(">>> 回答：" + content);
        return content;
    }

    @RequestMapping(value = "/chat", produces = MediaType.TEXT_HTML_VALUE)
    public String chatPage(@RequestParam String message) {
        // 使用API调用聊天
        String content = chatClient.prompt()
                .user(message)
                .call()
                .content();

        System.out.println(">>> 问题: " + message);
        System.out.println(">>> 回答：" + content);

        return MarkdownUtil.toHtmlPage(content);
    }



}