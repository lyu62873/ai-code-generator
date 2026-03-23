package com.leyu.aicodegenerator.core.builder;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Build Project Async. */
@Slf4j
@Component
public class VueProjectBuilder {

    @Value("${app.builder.npm-path:}")
    private String npmPath;

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
    private boolean executeCommand(File workingDir, String[] command, int timeoutSeconds) {
        try {
            log.info("Executing command: {} on working directory: {}", String.join(" ", command), workingDir.getAbsolutePath());
            Process process = RuntimeUtil.exec(
                    null, workingDir, command
            );

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("Command execution timed out ({} seconds).", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Command executed successfully: {}", String.join(" ", command));
                return true;
            }  else {
                log.error("Command execution fail: {}, error message: {}", String.join(" ", command), exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Command execution fail: {}, error message: {}", String.join(" ", command), e.getMessage());
            return false;
        }
    }

/** Execute Npm Install. */
    private boolean executeNpmInstall(File projectDir) {
        return executeNpmCommandWithFallback(projectDir, new String[]{"install"}, 300);
    }

/** Execute Npm Build. */
    private boolean executeNpmBuild(File projectDir) {
        return executeNpmCommandWithFallback(projectDir, new String[]{"run", "build"}, 180);
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

    private List<String> resolveNpmExecutables() {
        Set<String> candidates = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(npmPath)) {
            candidates.add(npmPath.trim());
        }
        candidates.add(buildCommand("npm"));
        if (!isWindows()) {
            candidates.add("/usr/bin/npm");
            candidates.add("/usr/local/bin/npm");
        }
        return new ArrayList<>(candidates);
    }

    private boolean executeNpmCommandWithFallback(File projectDir, String[] npmArgs, int timeoutSeconds) {
        List<String> candidates = resolveNpmExecutables();
        for (String candidate : candidates) {
            File candidateFile = new File(candidate);
            boolean pathLike = candidate.contains(File.separator) || candidateFile.isAbsolute();
            if (pathLike && (!candidateFile.exists() || !candidateFile.canExecute())) {
                log.warn("Skip npm executable candidate, file missing or not executable: {}", candidate);
                continue;
            }

            List<String[]> commandPlans = buildNpmCommandPlans(candidate, npmArgs);
            for (String[] command : commandPlans) {
                log.info("Trying npm command with executable candidate: {}", String.join(" ", command));
                boolean success = executeCommand(projectDir, command, timeoutSeconds);
                if (success) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String[]> buildNpmCommandPlans(String npmExecutable, String[] npmArgs) {
        List<String[]> plans = new ArrayList<>();
        plans.add(mergeCommand(npmExecutable, npmArgs));

        File npmFile = new File(npmExecutable);
        boolean absoluteNpmPath = npmFile.isAbsolute() && npmFile.getName().startsWith("npm");
        if (absoluteNpmPath) {
            try {
                Path binDir = npmFile.toPath().getParent();
                if (binDir != null) {
                    Path nodePath = binDir.resolve(isWindows() ? "node.exe" : "node");
                    Path npmCliPath = binDir.getParent()
                            .resolve("lib")
                            .resolve("node_modules")
                            .resolve("npm")
                            .resolve("bin")
                            .resolve("npm-cli.js");
                    File nodeFile = nodePath.toFile();
                    File npmCliFile = npmCliPath.toFile();
                    if (nodeFile.exists() && nodeFile.canExecute() && npmCliFile.exists() && npmCliFile.isFile()) {
                        String[] cliCommand = new String[npmArgs.length + 2];
                        cliCommand[0] = nodeFile.getAbsolutePath();
                        cliCommand[1] = npmCliFile.getAbsolutePath();
                        System.arraycopy(npmArgs, 0, cliCommand, 2, npmArgs.length);
                        plans.add(cliCommand);
                    }
                }
            } catch (Exception ignored) {
                // best-effort fallback command assembly only
            }
        }
        return plans;
    }

    private String[] mergeCommand(String executable, String[] args) {
        String[] command = new String[args.length + 1];
        command[0] = executable;
        System.arraycopy(args, 0, command, 1, args.length);
        return command;
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
