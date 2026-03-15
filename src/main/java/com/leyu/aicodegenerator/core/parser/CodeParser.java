package com.leyu.aicodegenerator.core.parser;

/**
 *
 * @param <T>
 */
public interface CodeParser<T> {

    /**
     *
     * @param codeContent
     * @return
     */
    T parseCode(String codeContent);
}
