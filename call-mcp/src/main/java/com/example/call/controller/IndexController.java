package com.example.call.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @Value("${project.base-url}")
    private String baseUrl;

    // 多个链接入口
    @GetMapping(value = {"/", "/index", "/index1"})
    public String chat1(Model model) {
        model.addAttribute("baseUrl", baseUrl);
        return "index1";
    }

    @GetMapping("/index2")
    public String chat2(Model model) {
        model.addAttribute("baseUrl", baseUrl);
        return "index2";
    }

}
