package com.leyu.aicodegenerator.ai;

import com.leyu.aicodegenerator.ai.model.HtmlCodeResult;
import com.leyu.aicodegenerator.ai.model.MultiFileCodeResult;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    private AiCodeGeneratorService htmlService;
    private AiCodeGeneratorService multiFileService;
    @BeforeEach
    void setUp() {
        long testAppId = 999001L; // 用固定测试ID即可
        htmlService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(testAppId, CodeGenTypeEnum.HTML);
        multiFileService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(testAppId + 1, CodeGenTypeEnum.MULTI_FILE);
    }


    @Test
    void generateHtmlCode() {
        HtmlCodeResult res = htmlService.generateHtmlCode("Make an web page to show a simple to-do list, keep everything as short as possible");
        Assertions.assertNotNull(res);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult res = multiFileService.generateMultiFileCode("Hello, I want to do an todo list, what tools should I use before start making page?");
        Assertions.assertNotNull(res);
    }
}