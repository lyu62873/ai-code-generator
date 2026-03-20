package com.leyu.aicodegenerator.utils;

import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/** FluxToCodeGenTypeUtil implementation. */
@Slf4j
public class FluxToCodeGenTypeUtil {

    public static CodeGenTypeEnum fluxToCodeGenType(Flux<String> flux) {

        return parseCodeGenType(FluxToStringUtil.fluxToString(flux));
    }

    /** parseCodeGenType implementation. */
    private static CodeGenTypeEnum parseCodeGenType(String raw) {
        if (raw == null || raw.isBlank()) {
            log.warn("Routing returned blank result, fallback to HTML");
            return CodeGenTypeEnum.HTML;
        }

        String normalized = raw.trim().toUpperCase();

        if (normalized.contains("VUE_PROJECT")) {
            return CodeGenTypeEnum.VUE_PROJECT;
        }
        if (normalized.contains("MULTI_FILE")) {
            return CodeGenTypeEnum.MULTI_FILE;
        }
        if (normalized.contains("HTML")) {
            return CodeGenTypeEnum.HTML;
        }

        log.warn("Routing returned unsupported value: {}, fallback to HTML", raw);
        return CodeGenTypeEnum.HTML;
    }
}
