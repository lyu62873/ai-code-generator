package com.leyu.aicodegenerator.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Run an AI-based code quality check for the provided content. */
@Service
public interface CodeQualityCheckService {

    /**
     * Check code quality.
     * The AI analyzes the code and returns the quality check result.
     */
    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    Flux<String> checkCodeQuality(@UserMessage String codeContent);
}
