package com.easipass.gateway.controller;

import com.easipass.commoncore.constant.SysErrorCode;
import com.easipass.commoncore.model.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("hystrix")
public class HystrixController {

    @GetMapping("fallback")
    public Mono<ApiResult> fallback() {
        return Mono.just(ApiResult.F(SysErrorCode.ERROR_CODE402));
    }

}
