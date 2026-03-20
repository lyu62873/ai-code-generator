package com.leyu.aicodegenerator.core.saver;

import com.leyu.aicodegenerator.ai.model.VueProjectCodeResult; // An empty model recommended in the previous step
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

/** Get Code Type. */
public class VueProjectCodeFileSaverTemplate extends CodeFileSaverTemplate<VueProjectCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.VUE_PROJECT;
    }

/** Add the provided record and persist it to storage. */
    @Override
    protected void saveFiles(VueProjectCodeResult result, String baseDirPath) {
    }

    @Override
    protected void validateInput(VueProjectCodeResult result) {
    }
}