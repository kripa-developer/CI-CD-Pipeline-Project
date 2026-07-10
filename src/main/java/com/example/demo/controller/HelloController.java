package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HelloController {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/")
    public Map<String, String> hello() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "Hello from Spring Boot CI/CD demo!");
        response.put("version", appVersion);
        response.put("hostname", hostname());
        return response;
    }

    @GetMapping("/api/version")
    public Map<String, String> version() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("version", appVersion);
        response.put("hostname", hostname());
        return response;
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
