package com.leyu.aicodegenerator.core;

import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File bioPage = aiCodeGeneratorFacade.generateAndSaveCode("bio page", CodeGenTypeEnum.HTML, 1L);
        Assertions.assertNotNull(bioPage);
    }

    @Test
    void generateAndSaveCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("to-do list website. User can write tasks and mark as complete/imcomplete. Keep it simple and short.", CodeGenTypeEnum.MULTI_FILE, 1L);

        List<String> result = codeStream.collectList().block();

        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

    @Test
    void generateVueProjectCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                "a simple to-do list website without image. Total lines of code should be less than 200",
                CodeGenTypeEnum.VUE_PROJECT, 2L);
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}