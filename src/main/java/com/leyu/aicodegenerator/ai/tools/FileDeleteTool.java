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

/**
 * File delete tool
 */
@Slf4j
@Component
public class FileDeleteTool extends BaseTool {

    @Tool("Delete the file in the specific path")
    public String deleteFile(
            @P("relative path of the file")
            String relativeFilePath,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path)) {
                return "Warn：File doesn't exist，no need to delete - " + relativeFilePath;
            }
            if (!Files.isRegularFile(path)) {
                return "Error：the path is not a file，cannot delete - " + relativeFilePath;
            }
            // safety check
            String fileName = path.getFileName().toString();
            if (isImportantFile(fileName)) {
                return "Error：cannot delete important file - " + fileName;
            }
            Files.delete(path);
            log.info("File deletion success: {}", path.toAbsolutePath());
            return "File deletion success: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "File deletion failed: " + relativeFilePath + ", error: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * check important files
     */
    private boolean isImportantFile(String fileName) {
        String[] importantFiles = {
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "vite.config.js", "vite.config.ts", "vue.config.js",
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md"
        };
        for (String important : importantFiles) {
            if (important.equalsIgnoreCase(fileName)) {
                return true;
            }
        }
        return false;
    }

/** Return the tool name exposed to the agent runtime. */
    @Override
    public String getToolName() {
        return "deleteFile";
    }

/** Return a user-facing display name for this tool. */
    @Override
    public String getDisplayName() {
        return "Delete File";
    }

/** Format the tool execution result for inclusion in chat history. */
    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[Tool Execution] %s %s", getDisplayName(), relativeFilePath);
    }
}
