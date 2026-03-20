package com.leyu.aicodegenerator.core.saver;

import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.ai.model.HtmlCodeResult;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

/**
 *
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

/** Add the provided record and persist it to storage. */
    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

/** Validate Input. */
    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);

        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML Code Content cannot be null");
        }
    }
}
