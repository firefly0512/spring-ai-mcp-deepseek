package com.example.call.config;

import io.modelcontextprotocol.client.McpClient;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class McpClientConfig implements McpSyncClientCustomizer {

    @Override
    public void customize(String name, McpClient.SyncSpec spec) {
        spec.requestTimeout(Duration.ofSeconds(30));
    }

}
