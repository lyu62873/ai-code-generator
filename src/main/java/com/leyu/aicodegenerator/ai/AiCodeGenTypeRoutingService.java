package com.leyu.aicodegenerator.ai;

import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

import dev.langchain4j.service.SystemMessage;

public interface AiCodeGenTypeRoutingService {

    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userPrompt);
}
