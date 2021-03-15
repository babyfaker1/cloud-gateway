package com.easipass.gateway.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MyLoadBalancerClientFilter implements GlobalFilter, Ordered {
    private static final Log log = LogFactory.getLog(MyLoadBalancerClientFilter.class);
    public static final int LOAD_BALANCER_CLIENT_FILTER_ORDER = 10099;

    public int getOrder() {
        return LOAD_BALANCER_CLIENT_FILTER_ORDER;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        if (url != null && ("lb".equals(url.getScheme()) || "lb".equals(schemePrefix))) {
            List isTest = new ArrayList();
            if(null != headers.get("isTest")){
                isTest = Collections.unmodifiableList(headers.get("isTest"));
            }
            if(isTest.size() > 0){
                String isTestValue = (String) isTest.get(0);
                if("1".equals(isTestValue)){
                    url.getPath();
                    try {
                        url = new URI(url.getScheme() + "://" + url.getHost() + url.getPath().substring(url.getPath().indexOf("/", url.getPath().indexOf("/") + 1)));
                    } catch (URISyntaxException e) {
                        log.error(e.getMessage() , e);
                    }
                }
            }
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, url);
        }
        return chain.filter(exchange);
    }
}
