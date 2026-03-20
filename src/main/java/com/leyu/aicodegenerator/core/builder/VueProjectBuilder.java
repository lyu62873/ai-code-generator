package com.leyu.aicodegenerator.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/** Build Project Async. */
@Slf4j
@Component
public class VueProjectBuilder {

    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildProject(projectPath);
            } catch (Exception e) {
                log.error("Async construction Vue project failed: {}", e.getMessage(), e);
            }
        });
    }

/** Execute Command. */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("Executing command: " + command + " on working directory: " + workingDir.getAbsolutePath());
            Process process = RuntimeUtil.exec(
                    null, workingDir, command.split("\\s+")
            );

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("Command execution timed out ({} seconds).", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Command executed successfully: {}", command);
                return true;
            }  else {
                log.error("Command execution fail: {}, error message: {}", command, exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Command execution fail: {}, error message: {}", command, e.getMessage());
            return false;
        }
    }

/** Execute Npm Install. */
    private boolean executeNpmInstall(File projectDir) {
        log.info("Executing npm installation");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300);
    }

/** Execute Npm Build. */
    private boolean executeNpmBuild(File projectDir) {
        log.info("Executing npm build");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180);
    }


/** Is Windows. */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

/** Build Project. */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("Project directory doesn't exist or is not a directory: {}", projectPath);
            return false;
        }

        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists() || !packageJson.isFile()) {
            log.error("package.json file doesn't exist or is not a file: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("Start building Vue project: {}", projectPath);
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install Failed");
            return false;
        }
        if (!executeNpmBuild(projectDir)) {
            log.error("npm build Failed");
            return false;
        }

        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("Construction complete but dist directory doesn't exist: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue project successfully constructed: {}", distDir.getAbsolutePath());
        return true;
    }
}
