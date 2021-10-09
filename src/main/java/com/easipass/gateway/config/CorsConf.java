package com.easipass.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
public class CorsConf {   //a1

    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter () {
        return new HiddenHttpMethodFilter() {
            public Mono<Void> filter (ServerWebExchange exchange, WebFilterChain chain) {
                return chain.filter(exchange);
            }
        };
    }


    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            ServerHttpRequest request = ctx.getRequest();
            String origin = ( null == request.getHeaders().get(HttpHeaders.ORIGIN) )
                    ? null
                    : request.getHeaders().get(HttpHeaders.ORIGIN).get(0);
            // 非跨域
            if (origin == null) {
                return chain.filter(ctx);
            }

            // 跨域
            ServerHttpResponse response = ctx.getResponse();
            HttpHeaders headers = response.getHeaders();
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, request.getHeaders().getOrigin());
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type,epToken,epSign,*");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,DELETE,PUT,OPTIONS,PATCH");
            if (request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }
            return chain.filter(ctx);
        };
    }
}
