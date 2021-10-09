package com.easipass.gateway.config;

import com.alibaba.fastjson.JSONObject;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.easipass.commoncore.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class GatewayOauthAppConf {    //a2

    private static JSONObject relations;

    public static JSONObject getRelations() {
        return relations;
    }

    public static void setRelations(JSONObject relations) {
        GatewayOauthAppConf.relations = relations;
    }

    @PostConstruct
    public void init() {
        ConfigFile config = ConfigService.getConfigFile("gatewayOauthAppConf" , ConfigFileFormat.JSON);
        changeConf(config.getContent());
        config.addChangeListener((ConfigFileChangeEvent changeEvent) -> {
            log.info("Changes for namespace " + changeEvent.getNamespace());
            log.info(String.format("Found change - oldValue: %s, newValue: %s, changeType: %s", changeEvent.getOldValue(), changeEvent.getNewValue(), changeEvent.getChangeType()));
            changeConf(changeEvent.getNewValue());
        });
    }

    private static void changeConf(String value){
        relations = JsonUtils.jsonToBean(value , JSONObject.class);
    }
}
