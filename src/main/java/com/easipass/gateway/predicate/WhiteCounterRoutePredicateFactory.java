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
public class WhiteCounterRoutePredicateFactory extends AbstractRoutePredicateFactory<BlackCounterRoutePredicateFactory.Config> {

    public WhiteCounterRoutePredicateFactory() {
        super(BlackCounterRoutePredicateFactory.Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("countGroup" , "maxCount");
    }

    @Override
    public Predicate<ServerWebExchange> apply(BlackCounterRoutePredicateFactory.Config config) {
        return BlackCounterRoutePredicateFactory.createCounterPredicate(config , false);
    }


}
