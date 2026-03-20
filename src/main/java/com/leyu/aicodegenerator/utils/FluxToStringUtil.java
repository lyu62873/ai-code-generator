package com.leyu.aicodegenerator.utils;

import reactor.core.publisher.Flux;

/** FluxToStringUtil implementation. */
public class FluxToStringUtil {

    public static String fluxToString(Flux<String> flux) {
        return flux
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .defaultIfEmpty("")
                .block();
    }
}
