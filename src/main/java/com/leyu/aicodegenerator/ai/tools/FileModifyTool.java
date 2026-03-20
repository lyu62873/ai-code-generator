package com.leyu.aicodegenerator.ai.tools;

import cn.hutool.json.JSONObject;
import com.leyu.aicodegenerator.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 *
 */
@Slf4j
@Component
public class FileModifyTool extends BaseTool {

    @Tool("Modify file content by replacing specific old content with new content.")
    public String modifyFile(
            @P("The relative path of the file.")
            String relativeFilePath,
            @P("The specific original content to be replaced.")
            String oldContent,
            @P("The new content to replace the old content with.")
            String newContent,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "Error：File not exist or not a file - " + relativeFilePath;
            }
            String originalContent = Files.readString(path);
            if (!originalContent.contains(oldContent)) {
                return "Warn：The content to be replaced was not found in the file; the file remains unmodified - " + relativeFilePath;
            }
            String modifiedContent = originalContent.replace(oldContent, newContent);
            if (originalContent.equals(modifiedContent)) {
                return "Status: No changes detected. The new content is identical to the existing content - " + relativeFilePath;
            }
            Files.writeString(path, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("File modification success: {}", path.toAbsolutePath());
            return "File modification success: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "File modification failed: " + relativeFilePath + ", error: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

/** Return the tool name exposed to the agent runtime. */
    @Override
    public String getToolName() {
        return "modifyFile";
    }

/** Return a user-facing display name for this tool. */
    @Override
    public String getDisplayName() {
        return "File Modification";
    }

/** Format the tool execution result for inclusion in chat history. */
    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String oldContent = arguments.getStr("oldContent");
        String newContent = arguments.getStr("newContent");
        return String.format("""
                [Tool Executed] %s %s
                
                Original:
                ```
                %s
                ```
                
                Replaced by:
                ```
                %s
                ```
                
                """, getDisplayName(), relativeFilePath, oldContent, newContent);
    }
}
