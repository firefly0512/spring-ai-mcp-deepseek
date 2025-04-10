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
                .defaultSystem("你是一个图书管理助手，可以帮助用户查询图书信息。" +
                        "你可以根据书名模糊查询、根据作者查询和根据分类查询图书。" +
                        "回复时，请使用简洁友好的语言，并将图书信息整理为易读的格式。")
                // 注册工具方法
                .defaultTools(toolCallbackProvider)
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