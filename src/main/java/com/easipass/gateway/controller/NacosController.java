package com.easipass.gateway.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.easipass.commoncore.model.ApiResult;
import com.easipass.commoncore.util.HttpUtils;
import com.easipass.commoncore.util.JsonUtils;
import com.easipass.commoncore.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequestMapping("nacos")
@RestController
public class NacosController extends NacosBaseController {

    @Value("${spring.application.name}")
    String gatewayName;

    @GetMapping("refreshAllGateway")
    public Mono<ApiResult> refreshAllGateway(ServerWebExchange exchange) throws NacosException {
        JSONObject instances = super.getCatalogInstances("1" , "999" , gatewayName);
        instances.getJSONArray("list").stream().forEach(instance -> {
            HttpUtils.getInstance().sendHttpPost("http://" + ((JSONObject)instance).getString("ip") + ":" + ((JSONObject)instance).getString("port") + "/actuator/gateway/refresh");
        });
        return Mono.just(ApiResult.T(null));
    }

    @GetMapping("service/list")
    public Mono<ApiResult> serviceList(ServerWebExchange exchange) throws NacosException {
        String pageNo = exchange.getRequest().getQueryParams().getFirst("pageNo");
        String pageSize = exchange.getRequest().getQueryParams().getFirst("pageSize");
        String keyword = exchange.getRequest().getQueryParams().getFirst("keyword");
        if(StringUtils.isEmpty(keyword)){
            keyword = "";
        }
        JSONObject jsonObject = super.getCatalogServices(pageNo , pageSize , keyword);
        return Mono.just(ApiResult.T(jsonObject));
    }

    @GetMapping("service")
    public Mono<ApiResult> service(ServerWebExchange exchange) throws NacosException {
        String serviceName = exchange.getRequest().getQueryParams().getFirst("serviceName");
        JSONObject jsonObject = super.getCatalogService(serviceName);
        return Mono.just(ApiResult.T(jsonObject));
    }

    @GetMapping("instance/list")
    public Mono<ApiResult> instanceList(ServerWebExchange exchange) throws NacosException {
        String pageNo = exchange.getRequest().getQueryParams().getFirst("pageNo");
        String pageSize = exchange.getRequest().getQueryParams().getFirst("pageSize");
        String serviceName = exchange.getRequest().getQueryParams().getFirst("serviceName");
        JSONObject instances = super.getCatalogInstances(pageNo , pageSize , serviceName);
        return Mono.just(ApiResult.T(instances));
    }

    @GetMapping("instance/put")
    public Mono<ApiResult> instance(ServerWebExchange exchange) throws NacosException {
        String serviceName = exchange.getRequest().getQueryParams().getFirst("serviceName");
        String ip = exchange.getRequest().getQueryParams().getFirst("ip");
        String port = exchange.getRequest().getQueryParams().getFirst("port");
        String metadata = exchange.getRequest().getQueryParams().getFirst("metadata");
        String enabled = exchange.getRequest().getQueryParams().getFirst("enabled");
        String weight = exchange.getRequest().getQueryParams().getFirst("weight");
        super.putInstance(serviceName , ip , port , metadata , enabled , weight);
        return Mono.just(ApiResult.T(null));
    }

    @GetMapping("mapping/list")
    public Mono<ApiResult> mappingList(ServerWebExchange exchange) throws NacosException {
        String ip = exchange.getRequest().getQueryParams().getFirst("ip");
        String port = exchange.getRequest().getQueryParams().getFirst("port");
        String contextPath = exchange.getRequest().getQueryParams().getFirst("context-path");
        if("${server.servlet.context-path}".equals(contextPath) || "${server.context-path}".equals(contextPath)){
            contextPath = "/";
        }
        String result = HttpUtils.getInstance().sendHttpGet("http://" + ip + ":" + port + contextPath + "/actuator/getAllMapping");
        ApiResult mappings = JsonUtils.jsonToBean(result , ApiResult.class);
        return Mono.just(mappings);
    }


}
