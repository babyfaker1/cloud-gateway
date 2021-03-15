package com.easipass.sys.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.easipass.commoncore.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@Slf4j
public class GatewayConfiguration {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public GatewayConfiguration(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }


    /**
     * 配置SentinelGatewayBlockExceptionHandler，限流后异常处理
     * @return
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public JsonSentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
//        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
        return new JsonSentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 配置SentinelGatewayFilter
     * @return
     */
    @Bean
    @Order(-1)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }


    /**
     * 配置限流规则
     */
    public static void initGatewayRules(JSONArray apolloFlowRules) {
        Set<GatewayFlowRule> gatewayFlowRules = new HashSet<>();
        for(Object obj : apolloFlowRules){
            gatewayFlowRules.add(JsonUtils.beanToBean(obj , GatewayFlowRule.class));
        }
        GatewayRuleManager.loadRules(gatewayFlowRules);
    }

    /**
     * 配置Api限流规则
     */
    public static void initApiGatewayRules(JSONObject appFlowMap){
        Set<GatewayFlowRule> gatewayFlowRules = new HashSet<>();
        Set<ApiDefinition> definitions = new HashSet<>();
//        Set<GatewayFlowRule> gatewayFlowRules = GatewayRuleManager.getRules();//获取原先的限流规则
//        Set<ApiDefinition> definitions = GatewayApiDefinitionManager.getApiDefinitions();//获取原先的资源定义
        log.info("old flowRules：" + JsonUtils.beanToJson(gatewayFlowRules));
        log.info("old definitions：" + JsonUtils.beanToJson(definitions));
        if(null != appFlowMap && appFlowMap.keySet().size() > 0){
            for(String app : appFlowMap.keySet()){
                JSONArray apolloFlowRules = appFlowMap.getJSONArray(app);
                for(Object obj : apolloFlowRules){
                    JSONObject jsonObject = (JSONObject) obj;
                    Object apis = jsonObject.remove("apis");
                    GatewayFlowRule gatewayFlowRule = JsonUtils.beanToBean(jsonObject , GatewayFlowRule.class);
                    if(null != apis){
                        gatewayFlowRule.setResource(app + "_" + gatewayFlowRule.getResource());//防止资源名称重复
                        JSONArray apiDefinitionsArray = (JSONArray) apis;
                        initCustomizedApis(definitions , gatewayFlowRule , apiDefinitionsArray);
                    }
                    gatewayFlowRules.add(gatewayFlowRule);
                }
            }
        }
        if(gatewayFlowRules.size() > 0){
            GatewayRuleManager.loadRules(gatewayFlowRules);//设置最新的限流规则
        }
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);//设置最新的资源定义
        log.info("new flowRules：" + JsonUtils.beanToJson(GatewayRuleManager.getRules()));
        log.info("new definitions：" + JsonUtils.beanToJson(GatewayApiDefinitionManager.getApiDefinitions()));
    }

    //自定义分组代码：
    private static void initCustomizedApis(Set<ApiDefinition> definitions , GatewayFlowRule gatewayFlowRule , JSONArray apiDefinitionsArray) {
        HashSet<ApiPredicateItem> apiPredicateItems = new HashSet<>();
        for(Object obj : apiDefinitionsArray){
            apiPredicateItems.add(JsonUtils.beanToBean(obj , ApiPathPredicateItem.class));
        }
        ApiDefinition apiDefinition = new ApiDefinition(gatewayFlowRule.getResource()).setPredicateItems(apiPredicateItems);
        definitions.add(apiDefinition);
    }

}