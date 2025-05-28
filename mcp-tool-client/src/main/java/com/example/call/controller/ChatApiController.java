package com.example.call.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
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

    private final ToolCallback[] registeredTools;

    private final MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

    public ChatApiController(ChatClient.Builder chatClientBuilder,
                             SyncMcpToolCallbackProvider toolProvider,
                             ToolCallingManager toolCallingManager) {

        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(toolProvider)
                .defaultSystem("你是一个MCP小助手，可根据需求调用不同的MCP工具，优化回答效果")
                .build();

        this.toolCallingManager = toolCallingManager;

        // 读取已注册的 tools 列表
        this.registeredTools = toolProvider.getToolCallbacks();
        Arrays.stream(registeredTools).toList().forEach(tool -> {
            log.info("工具名称: " + tool.getToolDefinition().name() + ", 功能描述: " + tool.getToolDefinition().description());
        });
    }

    @GetMapping(value = "/tools")
    public ToolCallback[] getAllTools() {
        return this.registeredTools;
    }

    @PostMapping(value = "/call")
    public List<Message> chatCall(@RequestParam("chatId") String chatId,
                                  @RequestParam("message") String message) {

        log.info(">>> chatId: " + chatId);
        log.info(">>> 问题: " + message);

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false) // 设置 false，可自行控制对 tool 的调用过程，方便调试
                .build();

        processAddMessageToChatMemory(chatId, List.of(new UserMessage(message)));

        Prompt chatPrompt = new Prompt(chatMemory.get(chatId), chatOptions);

        ChatResponse chatResponse = this.chatClient
                .prompt(chatPrompt)
                .call()
                .chatResponse();

        List<Message> messageList = new ArrayList<>();

        // 判断是否有工具调用，并循环调用工具
        while (chatResponse.hasToolCalls()) {
            AssistantMessage aiMessage = chatResponse.getResult().getOutput();
            log.info(">>> 输出内容：" + aiMessage.getText());

            List<AssistantMessage.ToolCall> toolCalls = aiMessage.getToolCalls();
            log.info(">>> {} 个调用工具", toolCalls.size());
            toolCalls.forEach(toolCall -> {
                log.info(">>> 准备调用工具{}：{}，参数：({})", toolCall.type(), toolCall.name(), toolCall.arguments());
            });

            ToolExecutionResult toolExecutionResult = null;
            try {
                toolExecutionResult = toolCallingManager.executeToolCalls(chatPrompt, chatResponse);
            } catch (Exception e) {
                log.error(">>> 调用工具失败", e);
                messageList.add(new AssistantMessage("调用工具失败:" + e.getMessage()));
                break;
            }

            List<Message> toolResultMessages  = toolExecutionResult.conversationHistory();

            log.info(">>> 调用了工具，执行结果" + toolResultMessages);

            Message lastMessage = toolResultMessages.get(toolResultMessages.size() - 1);
            if (lastMessage.getMessageType() == MessageType.TOOL) {
                ToolResponseMessage toolMessage = (ToolResponseMessage) lastMessage;

                // toolCalls 变成 map 格式
                toolMessage.getMetadata().put("toolArguments", toolCalls);

                messageList.add(toolMessage);

                for (ToolResponseMessage.ToolResponse resp :  toolMessage.getResponses()) {
                    log.info(">>> 工具ID：" + resp.id());
                    log.info(">>> 工具name：" + resp.name());
                    log.info(">>> 工具data：" + resp.responseData());
                }
            }

            processAddMessageToChatMemory(chatId, toolResultMessages);

            chatPrompt = new Prompt(chatMemory.get(chatId), chatOptions);

            chatResponse = this.chatClient
                    .prompt(chatPrompt)
                    .call()
                    .chatResponse();
        }

        messageList.add(chatResponse.getResult().getOutput());

        return messageList;
    }

    @GetMapping(value = "/generate_stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Message> generateStream(@RequestParam("chatId") String chatId,
                                        @RequestParam("message") String message) {

        log.info(">>> chatId: " + chatId);
        log.info(">>> 问题: " + message);

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
//                .internalToolExecutionEnabled(false) // 设置 false，可自行控制对 tool 的调用过程
                .build();

        processAddMessageToChatMemory(chatId, List.of(new UserMessage(message)));

        Prompt chatPrompt = new Prompt(chatMemory.get(chatId), chatOptions);

        Flux<ChatResponse> chatResponseFlux = this.chatClient
                .prompt(chatPrompt)
                .stream()
                .chatResponse();

        Flux<Message> returnMessages = chatResponseFlux.map(chat -> chat.getResult().getOutput());

        return returnMessages;
    }


    /**
     * 处理 Add ChatMemory, tool 工具类消息必须成对出现
     */
    private void processAddMessageToChatMemory(String chatId, List<Message> messages) {
        chatMemory.add(chatId, messages);

        List<Message> memoryMessage = chatMemory.get(chatId);

        long assistantCount = memoryMessage.stream().filter(m -> m.getMessageType() == MessageType.ASSISTANT).count();
        long toolCount = memoryMessage.stream().filter(m -> m.getMessageType() == MessageType.TOOL).count();

        if (assistantCount != toolCount) {
            log.error("tool 工具消息不成对，重置 memory list");
            chatMemory.clear(chatId);
            chatMemory.add(chatId, memoryMessage.subList(1, memoryMessage.size()));
        }
    }
}
