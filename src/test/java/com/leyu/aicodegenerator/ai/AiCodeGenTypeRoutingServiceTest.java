package com.leyu.aicodegenerator.ai;

import com.leyu.aicodegenerator.ai.model.CodeGenTypeRoutingResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@SpringBootTest
public class AiCodeGenTypeRoutingServiceTest {

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Test
    public void testRouteCodeGenType() {
        String userPrompt = "Make a simple bio page";
        Flux<String> result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        String raw = aiCodeGenTypeRoutingService
                .routeCodeGenType(userPrompt)
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .block();
        log.info("User need: {} -> {}", userPrompt, raw);
        userPrompt = "Make a company website with Main page, About us page, and Contact us page";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("User need: {} -> {}", userPrompt, result);
        userPrompt = "Build an e-commerce management system including user management, product management, and order management; routing and state management are required.";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("User need: {} -> {}", userPrompt, result);
    }
}
