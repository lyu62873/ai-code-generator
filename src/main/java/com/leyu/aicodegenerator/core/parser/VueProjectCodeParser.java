package com.leyu.aicodegenerator.core.parser;

import com.leyu.aicodegenerator.ai.model.VueProjectCodeResult;

/** Parse Code. */
public class VueProjectCodeParser implements CodeParser<VueProjectCodeResult> {
    @Override
    public VueProjectCodeResult parseCode(String codeContent) {
        return new VueProjectCodeResult();
    }
}