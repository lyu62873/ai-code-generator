package com.leyu.aicodegenerator.langgraph4j.node;

import com.leyu.aicodegenerator.ai.ImageCollectionService;
import com.leyu.aicodegenerator.langgraph4j.state.ImageResource;

import com.leyu.aicodegenerator.langgraph4j.state.WorkflowContext;
import com.leyu.aicodegenerator.model.enums.ImageCategoryEnum;
import com.leyu.aicodegenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;


@Slf4j
public class ImageCollectorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = "";
            
            try {
                ImageCollectionService imageCollectionService = SpringContextUtil.getBean(ImageCollectionService.class);
                Flux<String> stringFlux = imageCollectionService.collectImages(originalPrompt);
                imageListStr = fluxToString(stringFlux);
            } catch (Exception e) {
                log.error("Error in ImageCollectorNode: {}", e.getMessage(), e);
            }

            context.setCurrentStep("图片收集");
            context.setImageListStr(imageListStr);
            return WorkflowContext.saveContext(context);
            

            

        });
    }

    private static String fluxToString(Flux<String> stringFlux) {
        return stringFlux.reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .block();
    }
}
