package com.example.call;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CallMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallMcpApplication.class, args);
    }

}
