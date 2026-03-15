package com.leyu.aicodegenerator.core.parser;

import com.leyu.aicodegenerator.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     *
     * @param codeContent
     * @return
     */
    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult htmlCodeResult = new HtmlCodeResult();
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            htmlCodeResult.setHtmlCode(htmlCode.trim());
        } else {
            htmlCodeResult.setHtmlCode(codeContent.trim());
        }
        return htmlCodeResult;

    }

    /**
     *
     * @param content
     * @return
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) return matcher.group(1);
        return null;
    }
}


