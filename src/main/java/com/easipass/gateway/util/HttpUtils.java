package com.easipass.gateway.util;

import com.easipass.commoncore.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
public class HttpUtils {

    public static String getParamByExchange (ServerWebExchange exchange , String[] paramNames) {
        String paramValue = null;
        ServerHttpRequest request = exchange.getRequest();
        MultiValueMap<String , String> requestParemeters = request.getQueryParams();
        HttpHeaders httpHeaders = request.getHeaders();
        for(String paramName : paramNames){
            if(StringUtils.isEmpty(paramValue)){
                paramValue = requestParemeters.getFirst(paramName);
            }
            if(StringUtils.isEmpty(paramValue)){
                paramValue = httpHeaders.getFirst(paramName);
            }
            if(!StringUtils.isEmpty(paramValue)){
                log.info("paramName IS: " + paramName + ",paramValue IS: " + paramValue);
                break;
            }
        }
        return paramValue;
    }

   /* public static String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest) throws UnsupportedEncodingException {
        //获取请求体
        Flux<DataBuffer> body = serverHttpRequest.getBody();
        StringBuilder sb = new StringBuilder();
        body.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            String bodyString = new String(bytes, StandardCharsets.UTF_8);
            sb.append(bodyString);
        });
        //System.out.println(sb.toString());
        return URLDecoder.decode(sb.toString(),"utf-8");
    }*/
}
