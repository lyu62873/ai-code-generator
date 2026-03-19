package com.leyu.aicodegenerator.langgraph4j.node;

import com.leyu.aicodegenerator.core.builder.VueProjectBuilder;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.langgraph4j.state.WorkflowContext;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.leyu.aicodegenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ProjectBuilderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("Execute ProjectBuilderNode");

            // 获取必要的参数
            String generatedCodeDir = context.getGeneratedCodeDir();
            String buildResultDir;
            // Vue 项目类型：使用 VueProjectBuilder 进行构建
            try {
                VueProjectBuilder vueBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                // 执行 Vue 项目构建（npm install + npm run build）
                boolean buildSuccess = vueBuilder.buildProject(generatedCodeDir);
                if (buildSuccess) {
                    // 构建成功，返回 dist 目录路径
                    buildResultDir = generatedCodeDir + File.separator + "dist";
                    log.info("Vue Project build success，dist directory: {}", buildResultDir);
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue project build failed");
                }
            } catch (Exception e) {
                log.error("Vue project build error: {}", e.getMessage(), e);
                buildResultDir = generatedCodeDir; // 异常时返回原路径
            }


            // 更新状态
            context.setCurrentStep("项目构建");
            context.setBuildResultDir(buildResultDir);
            log.info("ProjectBuilderNode complete，directory: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}

