package com.leyu.aicodegenerator.ai;

import com.leyu.aicodegenerator.ai.model.HtmlCodeResult;
import com.leyu.aicodegenerator.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult res = aiCodeGeneratorService.generateHtmlCode("Make an web page to show a simple to-do list, keep everything as short as possible");
        Assertions.assertNotNull(res);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult res = aiCodeGeneratorService.generateMultiFileCode("Hello, I want to do an todo list, what tools should I use before start making page?");
        Assertions.assertNotNull(res);
    }
}