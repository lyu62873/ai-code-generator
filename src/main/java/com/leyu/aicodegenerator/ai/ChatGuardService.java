package com.leyu.aicodegenerator.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/** Method used by this component. */
public interface ChatGuardService {

    @SystemMessage(fromResource = "prompt/chat-guard-system-prompt.txt")
    Flux<String> guard(
            @UserMessage String userPrompt,
            @UserMessage String currentMode
    );
}