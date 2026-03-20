package com.leyu.aicodegenerator.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Route whether image collection is needed based on the user prompt. */
@Service
public interface ImageCollectionRoutingService {

    @SystemMessage(fromResource = "prompt/image-collection-routing-system-prompt.txt")
    Flux<String> routeImageCollection(@UserMessage String userPrompt);
}