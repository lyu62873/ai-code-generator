package com.leyu.aicodegenerator.ai;

import com.leyu.aicodegenerator.ai.model.CodeGenTypeRoutingResult;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

import dev.langchain4j.service.SystemMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Route the request to the correct code-generation type. */
@Service
public interface AiCodeGenTypeRoutingService {

    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    Flux<String> routeCodeGenType(String userPrompt);
}
