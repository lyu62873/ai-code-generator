package com.leyu.aicodegenerator.core.saver;

import com.leyu.aicodegenerator.ai.model.VueProjectCodeResult; // 这是上一步建议你创建的空模型
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

public class VueProjectCodeFileSaverTemplate extends CodeFileSaverTemplate<VueProjectCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.VUE_PROJECT;
    }

    @Override
    protected void saveFiles(VueProjectCodeResult result, String baseDirPath) {
    }

    @Override
    protected void validateInput(VueProjectCodeResult result) {
    }
}