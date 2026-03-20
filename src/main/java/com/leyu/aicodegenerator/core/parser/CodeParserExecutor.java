package com.leyu.aicodegenerator.core.parser;

import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

/**
 *
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();
/** Vue Project Code Parser. */
    private static final VueProjectCodeParser vueProjectCodeParser = new VueProjectCodeParser();

    /**
     *
     * @param codeContent
     * @param codeGenType
     * @return
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            case VUE_PROJECT -> vueProjectCodeParser.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsupported generation type：" + codeGenType);
        };
    }
}
