package com.leyu.aicodegenerator.ai;

import com.leyu.aicodegenerator.ai.model.CodeGenTypeRoutingResult;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGenTypeRoutingService {

    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    Flux<String> routeCodeGenType(String userPrompt);
}
