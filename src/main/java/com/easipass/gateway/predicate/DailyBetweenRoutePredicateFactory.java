package com.easipass.gateway.predicate;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;

import javax.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Slf4j
@Component
@Order(1)
public class DailyBetweenRoutePredicateFactory extends AbstractRoutePredicateFactory<DailyBetweenRoutePredicateFactory.Config> {

    /**
     * Time 1 key.
     */
    public static final String TIME1_KEY = "time1";

    /**
     * Time 2 key.
     */
    public static final String TIME2_KEY = "time2";

    public DailyBetweenRoutePredicateFactory() {
        super(DailyBetweenRoutePredicateFactory.Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        LocaleContextHolder.setLocale(new Locale("en"));
        return Arrays.asList(TIME1_KEY, TIME2_KEY);
    }

    @Override
    public Predicate<ServerWebExchange> apply(DailyBetweenRoutePredicateFactory.Config config) {
        Assert.isTrue(config.getTime1().isBefore(config.getTime2()),
                config.getTime1() + " must be before " + config.getTime2());

        return new GatewayPredicate() {
            @Override
            public boolean test(ServerWebExchange serverWebExchange) {
                final LocalTime now = LocalTime.now();
                return now.isAfter(config.getTime1())
                        && now.isBefore(config.getTime2());

            }

            @Override
            public String toString() {
                return String.format("Between: %s and %s", config.getTime1(),
                        config.getTime2());
            }
        };
    }


    @Validated
    public static class Config {

        @NotNull
        private LocalTime time1;

        @NotNull
        private LocalTime time2;

        public LocalTime getTime1() {
            return time1;
        }

        public DailyBetweenRoutePredicateFactory.Config setTime1(LocalTime time1) {
            this.time1 = time1;
            return this;
        }

        public LocalTime getTime2() {
            return time2;
        }

        public DailyBetweenRoutePredicateFactory.Config setTime2(LocalTime time2) {
            this.time2 = time2;
            return this;
        }

    }
}
