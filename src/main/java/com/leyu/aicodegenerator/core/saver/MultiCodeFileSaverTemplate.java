package com.leyu.aicodegenerator.core.saver;

import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.ai.model.MultiFileCodeResult;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

/**
 *
 */
public class MultiCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

/** Add the provided record and persist it to storage. */
    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

/** Validate Input. */
    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);

        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML Code Content cannot be null");
        }
    }
}
