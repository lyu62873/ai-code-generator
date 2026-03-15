package com.leyu.aicodegenerator.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.leyu.aicodegenerator.constant.AppConstant;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 *
 * @param <T>
 */
public abstract class CodeFileSaverTemplate<T> {

    protected static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     *
     * @param result
     * @return
     */
    public final File saveCode(T result, Long appId) {
        validateInput(result);
        String baseDirPath = buildUniqueDir(appId);
        saveFiles(result, baseDirPath);

        return new File(baseDirPath);
    }

    /**
     *
     * @param result
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Code Result cannot be null");
        }
    }

    /**
     *
     * @return
     */
    protected final String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Application ID cannot be null");
        }
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     *
     * @param dirPath
     * @param filename
     * @param content
     */
    protected final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     *
     * @return
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     *
     * @param result
     * @param baseDirPath
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
