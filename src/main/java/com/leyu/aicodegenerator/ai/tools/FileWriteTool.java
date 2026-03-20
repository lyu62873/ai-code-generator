package com.leyu.aicodegenerator.ai.tools;

import cn.hutool.core.io.FileUtil;
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

/** Write File. */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    @Tool("Write File to the specified path")
    public String writeFile(
            @P("Relative path of the file")
            String relativeFilePath,
            @P("Contents to write into file")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }

            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Successfully wrote file to the specified path: {}", path.toAbsolutePath());

            return "Successfully wrote file to the specified path: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "Failed to write file to the specified path: " + relativeFilePath + "\n" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

/** Return the tool name exposed to the agent runtime. */
    @Override
    public String getToolName() {
        return "writeFile";
    }

/** Return a user-facing display name for this tool. */
    @Override
    public String getDisplayName() {
        return "Write File";
    }

/** Format the tool execution result for inclusion in chat history. */
    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return String.format("""
                [Tool Executed] %s %s
                ```%s
                %s
                ```
                """, getDisplayName(), relativeFilePath, suffix, content);
    }
}
