package com.leyu.aicodegenerator.langgraph4j.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.langgraph4j.state.ImageResource;
import com.leyu.aicodegenerator.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class PromptEnhancerNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("Execute PromptEnhancerNode");
            
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = context.getImageListStr();
            List<ImageResource> imageList = context.getImageList();

            StringBuilder enhancedPromptBuilder = new StringBuilder();
            enhancedPromptBuilder.append(originalPrompt);

            if (CollUtil.isNotEmpty(imageList) || StrUtil.isNotEmpty(imageListStr)) {
                enhancedPromptBuilder.append("\n\n## Usable Image Resources\n");
                enhancedPromptBuilder.append("Please use the following image resources when generating the website, embedding them logically into the appropriate sections.\n");
                if (CollUtil.isNotEmpty(imageList)) {
                    for (ImageResource image : imageList) {
                        enhancedPromptBuilder.append("- ")
                                .append(image.getCategory().getText())
                                .append(": ")
                                .append("(")
                                .append(image.getUrl())
                                .append(")\n");
                    }
                } else enhancedPromptBuilder.append(imageListStr);
            }
            
            String enhancedPrompt = enhancedPromptBuilder.toString();

            context.setCurrentStep("提示词增强");
            context.setEnhancedPrompt(enhancedPrompt);
            log.info("Execute PromptEnhancerNode, the length is : {}", enhancedPrompt.length());
            return WorkflowContext.saveContext(context);
        });
    }
}
