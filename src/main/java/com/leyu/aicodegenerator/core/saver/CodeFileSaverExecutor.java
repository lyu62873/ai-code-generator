package com.leyu.aicodegenerator.core.saver;

import com.leyu.aicodegenerator.ai.model.HtmlCodeResult;
import com.leyu.aicodegenerator.ai.model.MultiFileCodeResult;
import com.leyu.aicodegenerator.ai.model.VueProjectCodeResult;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaver = new HtmlCodeFileSaverTemplate();
    private static final MultiCodeFileSaverTemplate multiFileSaver = new MultiCodeFileSaverTemplate();
    private static final VueProjectCodeFileSaverTemplate vueProjectCodeFileSaver = new VueProjectCodeFileSaverTemplate();


    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileSaver.saveCode((MultiFileCodeResult) codeResult, appId);
            case VUE_PROJECT -> vueProjectCodeFileSaver.saveCode((VueProjectCodeResult) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsupported generation type：" + codeGenType);
        };
    }
}
