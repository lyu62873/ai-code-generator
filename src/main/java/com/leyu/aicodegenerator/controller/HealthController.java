package com.leyu.aicodegenerator.controller;

import com.leyu.aicodegenerator.common.BaseResponse;
import com.leyu.aicodegenerator.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HealthController implementation. */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success();
    }
}
