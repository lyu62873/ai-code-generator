package com.leyu.aicodegenerator.core.handler;

import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.leyu.aicodegenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;
    @Resource
    private SimpleTextStreamHandler simpleTextStreamHandler;


    public Flux<String> doExecute(Flux<String> originFlux, long appId, User loginUser, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case VUE_PROJECT -> jsonMessageStreamHandler.handle(originFlux, appId, loginUser);
            case HTML, MULTI_FILE -> simpleTextStreamHandler.handle(originFlux, appId, loginUser);
        };
    }
}
