package com.leyu.aicodegenerator.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Image collection AI service interface.
 * Use AI to call tools and collect different types of image resources.
 */
@Service
public interface ImageCollectionService {

    /**
     * Collect required image resources based on the user prompt.
     * The AI will choose and call appropriate tools based on the request.
     */
    @SystemMessage(fromResource = "prompt/image-collection-system-prompt.txt")
    Flux<String> collectImages(@UserMessage String userPrompt);
}
