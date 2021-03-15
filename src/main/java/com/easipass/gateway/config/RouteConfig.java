package com.easipass.gateway.config;

import com.easipass.gateway.filter.ElapsedFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@Slf4j
@Configuration
public class RouteConfig {

//    @Bean
//    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
//
//        /*
//        route1 是get请求，get请求使用readBody会报错
//        route2 是post请求，Content-Type是application/x-www-form-urlencoded，readbody为String.class
//        route3 是post请求，Content-Type是application/json,readbody为Object.class
//         */
//        RouteLocatorBuilder.Builder routes = builder.routes();
//        RouteLocatorBuilder.Builder serviceProvider = routes
//                .route("route3",
//                        r -> r
//                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                                .and()
//                                .method(HttpMethod.POST)
//                                .and()
//                                .readBody(Object.class, readBody -> {
//                                    log.info("request method POST, Content-Type is application/json, body  is:{}", readBody);
//                                    return true;
//                                })
//                                .and()
//                                .path("/epoa/**")
//                                .filters(f -> {
//                                    return f;
//                                })
//                                .uri("http://192.168.12.99:7777/epoa/"));
//        RouteLocator routeLocator = serviceProvider.build();
//        log.info("custom RouteLocator is loading ... {}", routeLocator);
//        return routeLocator;
//    }
}
