package com.leyu.aicodegenerator.core.builder;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/** Build Project Async. */
@Slf4j
@Component
public class VueProjectBuilder {

    private static final String NODE_OPTIONS = "--max-old-space-size=384";
    private static final String[] NPM_INSTALL_FALLBACK_ARGS = {"install", "--no-audit", "--no-fund", "--prefer-offline"};
    private static final String[] NPM_CI_ARGS = {"ci", "--no-audit", "--no-fund", "--prefer-offline"};
    private static final ReentrantLock BUILD_LOCK = new ReentrantLock();

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
    private boolean executeCommand(File workingDir, String[] envp, String[] command, int timeoutSeconds) {
        try {
            log.info("Executing command: {} on working directory: {}", String.join(" ", command), workingDir.getAbsolutePath());
            Process process = RuntimeUtil.exec(
                    envp, workingDir, command
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Command execution interrupted: {}, error message: {}", String.join(" ", command), e.getMessage());
            return false;
        } catch (RuntimeException e) {
            log.error("Command execution fail: {}, error message: {}", String.join(" ", command), e.getMessage());
            return false;
        }
    }

/** Execute Npm Install. */
    private boolean executeNpmInstall(File projectDir) {
        File lockFile = new File(projectDir, "package-lock.json");
        if (lockFile.exists() && lockFile.isFile() && shouldSkipInstall(projectDir, lockFile)) {
            log.info("Skip dependency install for {}, lock hash unchanged", projectDir.getAbsolutePath());
            return true;
        }

        boolean success;
        if (lockFile.exists() && lockFile.isFile()) {
            success = executeNpmCommandWithFallback(projectDir, NPM_CI_ARGS, 300, null);
            if (!success) {
                log.warn("npm ci failed, fallback to npm install, project={}", projectDir.getAbsolutePath());
                success = executeNpmCommandWithFallback(projectDir, NPM_INSTALL_FALLBACK_ARGS, 300, null);
            }
        } else {
            success = executeNpmCommandWithFallback(projectDir, NPM_INSTALL_FALLBACK_ARGS, 300, null);
        }
        if (success && lockFile.exists() && lockFile.isFile()) {
            saveLockHash(projectDir, lockFile);
        }
        return success;
    }

/** Execute Npm Build. */
    private boolean executeNpmBuild(File projectDir) {
        String[] mergedEnv = mergeWithSystemEnv(new String[]{"NODE_OPTIONS=" + NODE_OPTIONS});
        return executeNpmCommandWithFallback(projectDir, new String[]{"run", "build"}, 180,
                mergedEnv);
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

    private boolean executeNpmCommandWithFallback(File projectDir, String[] npmArgs, int timeoutSeconds, String[] envp) {
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
                boolean success = executeCommand(projectDir, envp, command, timeoutSeconds);
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

    private String[] mergeWithSystemEnv(String[] extraEnvp) {
        if (extraEnvp == null || extraEnvp.length == 0) {
            return null;
        }
        Map<String, String> env = new LinkedHashMap<>(System.getenv());
        for (String entry : extraEnvp) {
            if (StrUtil.isBlank(entry)) {
                continue;
            }
            int splitIndex = entry.indexOf('=');
            if (splitIndex <= 0 || splitIndex == entry.length() - 1) {
                continue;
            }
            String key = entry.substring(0, splitIndex);
            String value = entry.substring(splitIndex + 1);

            if ("NODE_OPTIONS".equals(key) && StrUtil.isNotBlank(env.get(key))) {
                String existingValue = env.get(key);
                if (!existingValue.contains(value)) {
                    value = existingValue + " " + value;
                } else {
                    value = existingValue;
                }
            }
            env.put(key, value);
        }
        return env.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);
    }

    private boolean shouldSkipInstall(File projectDir, File lockFile) {
        try {
            File nodeModules = new File(projectDir, "node_modules");
            if (!nodeModules.exists() || !nodeModules.isDirectory()) {
                return false;
            }

            Path cachePath = getLockHashCachePath(projectDir);
            if (!Files.exists(cachePath) || !Files.isRegularFile(cachePath)) {
                return false;
            }

            String currentHash = sha256(lockFile.toPath());
            String cachedHash = Files.readString(cachePath).trim();
            return StrUtil.isNotBlank(currentHash) && currentHash.equals(cachedHash);
        } catch (Exception e) {
            log.warn("Failed checking install cache, fallback to install: {}", e.getMessage());
            return false;
        }
    }

    private void saveLockHash(File projectDir, File lockFile) {
        try {
            String hash = sha256(lockFile.toPath());
            if (StrUtil.isBlank(hash)) {
                return;
            }
            Path cachePath = getLockHashCachePath(projectDir);
            Files.createDirectories(cachePath.getParent());
            Files.writeString(cachePath, hash, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed writing install cache hash: {}", e.getMessage());
        }
    }

    private Path getLockHashCachePath(File projectDir) {
        return projectDir.toPath().resolve(".aicodegen").resolve("lock.sha256");
    }

    private String sha256(Path filePath) throws Exception {
        byte[] bytes = Files.readAllBytes(filePath);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

/** Build Project. */
    public boolean buildProject(String projectPath) {
        BUILD_LOCK.lock();
        try {
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
        } finally {
            BUILD_LOCK.unlock();
        }
    }
}
