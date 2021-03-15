package com.easipass.gateway.util;

import com.alibaba.fastjson.JSONObject;
import com.easipass.commoncore.constant.SysConstants;
import com.easipass.commoncore.constant.SysErrorCode;
import com.easipass.commoncore.model.ApiResult;
import com.easipass.commoncore.util.JsonUtils;
import com.easipass.gateway.constant.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
public class HandleExceptionUtils {

    public static Mono<Void> handleErrJsonResponse (SysErrorCode sysErrorCode, ServerWebExchange exchange) {
        return handleErrJsonResponse(sysErrorCode.getErrorCode() , sysErrorCode.getErrorInfo() , exchange);
    }

    public static Mono<Void> handleErrJsonResponse (ExceptionConstant sysErrorCode, ServerWebExchange exchange) {
        return handleErrJsonResponse(sysErrorCode.getErrorCode() , sysErrorCode.getErrorInfo() , exchange);
    }

    public static Mono<Void> handleErrJsonResponse (String errorCode, String errorInfo, ServerWebExchange exchange) {
        ApiResult apiResult = ApiResult.newInstance(SysConstants.FLAG_F, errorCode, errorInfo, "");
        JSONObject objRtn = JsonUtils.beanToBean(apiResult);
        objRtn.put("isGateway" , true);
        String jsonRtn = JsonUtils.beanToJson(objRtn);
        log.error(jsonRtn);
        assert jsonRtn != null;
        return handleJsonResponse(jsonRtn, exchange);
    }

    /***
     * 流输出
     * @param json 返回json串
     * @param exchange 请求全局事件总线
     * @return 返回
     */
    public static Mono<Void> handleJsonResponse (String json, ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        byte[] bits = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bits);
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        return response.writeWith(Mono.just(buffer));
    }


}
