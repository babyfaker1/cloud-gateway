package com.easipass.gateway.config;

import com.alibaba.fastjson.JSONObject;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.easipass.commoncore.util.JsonUtils;
import com.easipass.sys.config.GatewayConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class SentinelRulesConf {

    @PostConstruct
    public void init() {
        dynamicRouteByApolloListener();
    }

    public void dynamicRouteByApolloListener (){
        try {
            ConfigFile config = ConfigService.getConfigFile("SentinelRulesConf" , ConfigFileFormat.JSON);
            changeSentinelRules(config.getContent());
            config.addChangeListener((ConfigFileChangeEvent changeEvent) -> {
                log.info("Changes for namespace " + changeEvent.getNamespace());
                log.info(String.format("Found change - oldValue: %s, newValue: %s, changeType: %s", changeEvent.getOldValue(), changeEvent.getNewValue(), changeEvent.getChangeType()));
                changeSentinelRules(changeEvent.getNewValue());
            });
        } catch (Exception e) {
            log.error(e.getMessage() , e);
        }
    }

    private void changeSentinelRules(String value){
        JSONObject appFlowMap = JsonUtils.jsonToBean(value);
        GatewayConfiguration.initApiGatewayRules(appFlowMap);
    }
}
