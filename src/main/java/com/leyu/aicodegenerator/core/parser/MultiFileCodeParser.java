package com.leyu.aicodegenerator.core.parser;

import com.leyu.aicodegenerator.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    // Allow optional metadata after language tag, e.g. ```javascript script.js
    // to avoid missing code blocks when the model appends filename hints.
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html[^\\n]*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css[^\\n]*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
/** Compile. */
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)[^\\n]*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);


    /**
     *
     * @param codeContent
     * @return
     */
    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult multiFileCodeResult = new MultiFileCodeResult();

        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);

        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            multiFileCodeResult.setHtmlCode(htmlCode);
        }
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            multiFileCodeResult.setCssCode(cssCode);
        }
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            multiFileCodeResult.setJsCode(jsCode);
        }

        return multiFileCodeResult;

    }


/** Extract Code By Pattern. */
    private String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

