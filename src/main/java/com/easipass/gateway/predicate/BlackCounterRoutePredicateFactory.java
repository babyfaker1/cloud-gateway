package com.easipass.gateway.predicate;

import com.easipass.commoncore.model.ApiResult;
import com.easipass.gateway.controller.CounterController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;

import javax.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
@Component
@Order(1)
public class BlackCounterRoutePredicateFactory extends AbstractRoutePredicateFactory<BlackCounterRoutePredicateFactory.Config> {

    public BlackCounterRoutePredicateFactory() {
        super(BlackCounterRoutePredicateFactory.Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("countGroup" , "maxCount");
    }

    public static Predicate<ServerWebExchange> createCounterPredicate(BlackCounterRoutePredicateFactory.Config config , Boolean blackOrWhite){
        return exchange -> {
            Boolean predicateFlag = blackOrWhite;
            String countGroup = config.getCountGroup();
            int count = 0;
            ApiResult apiResult = CounterController.oauthRedisGet((blackOrWhite ? "BlackCounter:" : "WhiteCounter:") + countGroup);
            if("T".equals(apiResult.getFlag())){
                if(null != apiResult.getData()){
                    count = Integer.valueOf(String.valueOf(apiResult.getData()));
                }
                if(count + 1 <= (null == config.getMaxCount() ? 0 : Integer.valueOf(config.getMaxCount()))){
                    count ++;
                    if(blackOrWhite){//黑名单计数器只能每次都计数，而白名单计数器可以通过是否访问到接口来确定计数
                        CounterController.oauthRedisSet("BlackCounter:" + countGroup , String.valueOf(count));
                    }else {
                        exchange.getAttributes().put("PredicateName" , "WhiteCounter");
                        exchange.getAttributes().put("countGroup" , countGroup);
                        exchange.getAttributes().put("count" , String.valueOf(count));
                    }
                    predicateFlag = !blackOrWhite;
                }
            }
            log.info("CountGroup：" + countGroup);
            log.info("Count：" + count);
            return predicateFlag;
        };
    }

    @Override
    public Predicate<ServerWebExchange> apply(BlackCounterRoutePredicateFactory.Config config) {
        return createCounterPredicate(config , true);
    }

    @Validated
    public static class Config {

        @NotEmpty
        private String countGroup = null;
        private String maxCount = null;

        public String getCountGroup() {
            return countGroup;
        }

        public BlackCounterRoutePredicateFactory.Config setCountGroup(String countGroup) {
            this.countGroup = countGroup;
            return this;
        }

        public String getMaxCount() {
            return maxCount;
        }

        public BlackCounterRoutePredicateFactory.Config setMaxCount(String maxCount) {
            this.maxCount = maxCount;
            return this;
        }

    }
}
