package com.leyu.aicodegenerator.langgraph4j.node;

import com.leyu.aicodegenerator.ai.AiCodeGenTypeRoutingService;
import com.leyu.aicodegenerator.langgraph4j.state.WorkflowContext;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.leyu.aicodegenerator.utils.FluxToCodeGenTypeUtil;
import com.leyu.aicodegenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");
            
            CodeGenTypeEnum generationType;

            try {
                AiCodeGenTypeRoutingService routingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);

                Flux<String> flux = routingService.routeCodeGenType(context.getOriginalPrompt());

                generationType = FluxToCodeGenTypeUtil.fluxToCodeGenType(flux);
                log.info("AI routing complete, choose: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI routing error, {}. Using HTML", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }

            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}
