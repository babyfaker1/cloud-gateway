package com.easipass.gateway.filter;

import com.easipass.commoncore.model.ApiResult;
import com.easipass.commoncore.util.StringUtils;
import com.easipass.gateway.controller.CounterController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
@Slf4j
public class SaveSessionToRequestUriGatewayFilterFactory extends AbstractGatewayFilterFactory<SaveSessionToRequestUriGatewayFilterFactory.Config> {

    private static Random random = new Random();

    @Value("${settings.saveSessionSecond:3600}")
    private Integer saveSessionSecond;

    public SaveSessionToRequestUriGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("patterns");
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange , GatewayFilterChain chain) {
                Integer index = -1;
                HttpCookie jessionidCookie = exchange.getRequest().getCookies().getFirst("JSESSIONID");
                if(null != jessionidCookie){
                    String jessionid = jessionidCookie.getValue();
                    ApiResult apiResult = CounterController.oauthRedisGet("SaveSessionToRequestUri:" + jessionid );
                    if("T".equals(apiResult.getFlag())){
                        if(StringUtils.isEmpty(apiResult.getData())){//第一次的JSESSIONID
                            index = getRandomIntInRange(0 , config.getPatterns().size() - 1);
                        }else {//拿到之前的JSESSIONID对应的下标
                            index = Integer.valueOf(String.valueOf(apiResult.getData()));
                        }
                    }
                    log.info("jessionid: " + jessionid + " , saveSessionIndex: " + index);
                    CounterController.oauthRedisSetEx("SaveSessionToRequestUri:" + jessionid  , String.valueOf(index), saveSessionSecond);
                }else {//如果是无状态的请求或者是有状态的第一次请求
                    index = getRandomIntInRange(0 , config.getPatterns().size() - 1);
                }
                exchange.getAttributes().put("FilterName" , "SaveSessionToRequestUri");//为了处理响应头中的Set-Cookie
                exchange.getAttributes().put("saveSessionIndex" , index);//
                String requestUrl = index == -1 ? null : config.getPatterns().get(index);
                if(!StringUtils.isEmpty(requestUrl)){
                    Route oldRoute = (Route) exchange.getAttributes().get(GATEWAY_ROUTE_ATTR);
                    Route newRoute = Route.async().id(oldRoute.getId()).order(oldRoute.getOrder()).uri(URI.create(requestUrl)).predicate(swe -> true).build();
                    exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, newRoute);
                }
                if (log.isTraceEnabled()) {
                    log.trace("SaveSessionToRequestUri with: " + requestUrl);
                }
                return chain.filter(exchange);
            }

            @Override
            public String toString() {
                return filterToStringCreator(SaveSessionToRequestUriGatewayFilterFactory.this)
                        .append("SaveSessionToRequestUri", config.getPatterns()).toString();
            }
        };
    }

    @Validated
    public static class Config {

        private List<String> patterns = new ArrayList<>();

        @Deprecated
        public String getPattern() {
            if (!CollectionUtils.isEmpty(this.patterns)) {
                return patterns.get(0);
            }
            return null;
        }

        @Deprecated
        public SaveSessionToRequestUriGatewayFilterFactory.Config setPattern(String pattern) {
            this.patterns = new ArrayList<>();
            this.patterns.add(pattern);
            return this;
        }

        public List<String> getPatterns() {
            return patterns;
        }

        public SaveSessionToRequestUriGatewayFilterFactory.Config setPatterns(List<String> patterns) {
            this.patterns = patterns;
            return this;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this).append("patterns", patterns).toString();
        }

    }

    static int getRandomIntInRange(int min, int max) {
        return random.ints(min, (max + 1)).limit(1).findFirst().getAsInt();
    }

}
