package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

/** Of. */
@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     *
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     *
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    /**
     *
     *
     * @param projectRoot
     * @param fullPath
     * @return
     */
/** Is Path Allowed. */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {

        Path relativePath = projectRoot.relativize(fullPath);
        //
        for (Path part : relativePath) {
            String partName = part.toString();
            //
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            //
            if (IGNORED_EXTENSIONS.stream().anyMatch(partName::endsWith)) {
                return false;
            }
        }
        return true;
    }

/** Download Project As Zip. */
    @Override
    public void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response) {
        //
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR, "Path cannot be null");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR, "Download file name cannot be null");
        File projectDir = new File(projectPath);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.NOT_FOUND_ERROR, "Project directory does not exist");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR, "Path is not a directory");
        log.info("Start to package project: {} -> {}.zip", projectPath, downloadFileName);
        //
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s.zip\"", downloadFileName));
        //
        FileFilter filter = file -> isPathAllowed(projectDir.toPath(), file.toPath());
        try {
            //
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, filter, projectDir);
            log.info("Project package complete: {}", downloadFileName);
        } catch (Exception e) {
            log.error("Project package failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Project package failed");
        }
    }

}
