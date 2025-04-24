package com.example.call.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatApiController {

    private final ChatClient chatClient;

    private final ChatMemory chatMemory = new InMemoryChatMemory();

    public ChatApiController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        this.chatClient = chatClientBuilder
                .defaultTools(tools)
                .build();
    }

    @GetMapping(value = "/generate_stream")
    public Flux<ChatResponse> generateStream(HttpServletResponse response,
                                             @RequestParam("id") String id,
                                             @RequestParam("prompt") String prompt) {
        response.setCharacterEncoding("UTF-8");
        var messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, id, 10);
        return this.chatClient.prompt(prompt)
                .advisors(messageChatMemoryAdvisor)
                .stream()
                .chatResponse();
    }


    @GetMapping("/advisor/{id}/{prompt}")
    public Flux<String> advisorChat(HttpServletResponse response,
                                    @PathVariable String id,
                                    @PathVariable String prompt) {

        response.setCharacterEncoding("UTF-8");
        var messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, id, 10);
        return this.chatClient.prompt(prompt)
                .advisors(messageChatMemoryAdvisor)
                .stream()
                .content();
    }

}
