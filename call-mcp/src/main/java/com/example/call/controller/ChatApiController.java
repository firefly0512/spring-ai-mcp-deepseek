package com.example.call.controller;

import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatApiController {

    private final ChatClient chatClient;

    private final ToolCallingManager toolCallingManager;

    private final SyncMcpToolCallbackProvider toolProvider;
    private final ToolCallback[] registeredTools;

    public ChatApiController(ChatClient.Builder chatClientBuilder,
                             SyncMcpToolCallbackProvider toolProvider,
                             ToolCallingManager toolCallingManager) {


        this.chatClient = chatClientBuilder
                .defaultTools(toolProvider)
                .build();

        this.toolProvider = toolProvider;
        this.toolCallingManager = toolCallingManager;

        // 读取已注册的 tools 列表
        this.registeredTools = this.toolProvider.getToolCallbacks();
        Arrays.stream(registeredTools).toList().forEach(tool -> {
            log.info("工具名称: " + tool.getToolDefinition().name() + ", 功能描述: " + tool.getToolDefinition().description());
        });
    }

    @GetMapping(value = "/tools")
    public ToolCallback[] getAllTools() {
//        ToolCallback[] registeredTools = this.toolProvider.getToolCallbacks();
        return this.registeredTools;
    }

    @GetMapping(value = "/generate_stream")
    public Flux<Message> generateStream(HttpServletResponse response,
                                             @RequestParam("id") String id,
                                             @RequestParam("prompt") String prompt) {
        response.setCharacterEncoding("UTF-8");

        log.info(">>> id: " + id);
        log.info(">>> 问题: " + prompt);

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();

        Prompt chatPrompt = new Prompt(prompt, chatOptions);

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(new InMemoryChatMemory());

        Flux<ChatResponse> chatResponseFlux = this.chatClient
                .prompt(new Prompt(prompt, chatOptions))
                .advisors(messageChatMemoryAdvisor)
                .stream()
                .chatResponse();

        ChatResponse chatResponse = chatResponseFlux.blockLast();

        // 初始化返回页面的内容
        Flux<Message> messageFlux = Flux.just();

        if(!chatResponse.hasToolCalls()) {
            messageFlux = chatResponseFlux.map(chat -> chat.getResult().getOutput());
        }

        // 判断是否有工具调用，并循环调用工具
        while (chatResponse.hasToolCalls()) {
            AssistantMessage aiMessage = chatResponse.getResult().getOutput();
            log.info(">>> 输出内容：" + aiMessage.getText());

            List<AssistantMessage.ToolCall> toolCalls = aiMessage.getToolCalls();
            log.info(">>> {} 个调用工具", toolCalls.size());
            toolCalls.forEach(toolCall -> {
                log.info(">>> 准备调用工具{}：{}，参数：({})", toolCall.type(), toolCall.name(), toolCall.arguments());
            });

            aiMessage.getMedia().forEach(media -> {
                log.info(">>> 媒体类型：" + media.getMimeType());
            });

            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(chatPrompt, chatResponse);

            List<Message> toolResultMessages  = toolExecutionResult.conversationHistory();

            log.info(">>> 调用了工具，执行结果" + toolResultMessages);
//            toolResultMessages.forEach(message -> {
//                log.info(">>> 调用了工具，执行结果：" + message);
//            });

            Message lastMessage = toolResultMessages.get(toolResultMessages.size() - 1);
            if (lastMessage.getMessageType() == MessageType.TOOL) {
                ToolResponseMessage toolMessage = (ToolResponseMessage) lastMessage;

                // toolCalls 变成 map 格式
                toolMessage.getMetadata().put("toolArguments", toolCalls);

                messageFlux = Flux.concat(messageFlux, Flux.just(toolMessage));

                for (ToolResponseMessage.ToolResponse resp :  toolMessage.getResponses()) {
                    log.info(">>> 工具ID：" + resp.id());
                    log.info(">>> 工具name：" + resp.name());
                    log.info(">>> 工具data：" + resp.responseData());
                }
            }

            chatPrompt = new Prompt(toolResultMessages, chatOptions);

            Flux<ChatResponse> newChatResponseFlux = this.chatClient
                    .prompt(chatPrompt)
                    .stream()
                    .chatResponse();

            chatResponse = newChatResponseFlux.blockLast();

            if (!chatResponse.hasToolCalls()) {
                // messageFlux 追加新的内容
                messageFlux = Flux.concat(messageFlux, newChatResponseFlux.map(chat -> chat.getResult().getOutput()));
            }
        }

        return messageFlux;
    }

//    @PostMapping("/execute_tool")
//    public Flux<ChatResponse> executeTool(HttpServletResponse response,
//                                          @RequestParam("id") String id,
//                                          @RequestParam("toolName") String toolName,
//                                          @RequestBody String toolInput) {
//        response.setCharacterEncoding("UTF-8");
//        return this.chatClient.prompt()
//                .advisors(messageChatMemoryAdvisor)
//                .tools(toolName, toolInput)
//                .stream()
//                .chatResponse();
//    }
//
//    @GetMapping("/advisor/{id}/{prompt}")
//    public Flux<String> advisorChat(HttpServletResponse response,
//                                    @PathVariable String id,
//                                    @PathVariable String prompt) {
//
//        response.setCharacterEncoding("UTF-8");
//        return this.chatClient.prompt(prompt)
//                .advisors(messageChatMemoryAdvisor)
//                .stream()
//                .content();
//    }

}
