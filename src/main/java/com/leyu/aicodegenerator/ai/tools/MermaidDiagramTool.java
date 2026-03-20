package com.leyu.aicodegenerator.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.system.SystemUtil;
import com.leyu.aicodegenerator.entity.ImageResource;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.manager.CosManager;
import com.leyu.aicodegenerator.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Generate output for the request (and persist/upload as needed). */
@Slf4j
@Component
public class MermaidDiagramTool extends BaseTool {

    @Resource
    private CosManager cosManager;
    
    @Tool("Convert Mermaid code into an architecture diagram image to show system structure and technical relationships")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid chart code") String mermaidCode,
                                                      @P("Description of the architecture diagram") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // Convert to an SVG image
            File diagramFile = convertMermaidToSvg(mermaidCode);
            // Upload to COS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // Clean up temporary files
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to generate architecture diagram: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Convert Mermaid code into an SVG image.
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // Create a temporary input file
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        // Create a temporary output file
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        // Pick the command based on the OS
        String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
        // Build the command
        String cmdLine = String.format("%s -i %s -o %s -b transparent",
                command,
                tempInputFile.getAbsolutePath(),
                tempOutputFile.getAbsolutePath()
        );
        // Execute the command
        RuntimeUtil.execForStr(cmdLine);
        // Check the output file
        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI execution failed");
        }
        // Delete input file; keep the output file for upload
        FileUtil.del(tempInputFile);
        return tempOutputFile;
    }

/** Return the tool name exposed to the agent runtime. */
    @Override
    public String getToolName() {
        return "searchMermaidDiagramImages";
    }

/** Return a user-facing display name for this tool. */
    @Override
    public String getDisplayName() {
        return "Search Mermaid Diagram Images";
    }

/** Format the tool execution result for inclusion in chat history. */
    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String query = arguments.getStr("query");
        return String.format("[Tool Executed] %s query=%s", getDisplayName(), query);
    }
}
