package com.easipass.gateway.config;

import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.easipass.commoncore.util.JsonUtils;
import com.easipass.gateway.route.DynamicRouteServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class RouteEditor {

    private static Set<String> ROUTE_IDS = new HashSet<>();

    @Autowired
    private DynamicRouteServiceImpl dynamicRouteService;

    @PostConstruct
    public void init() {
        dynamicRouteByApolloListener();
    }

    public void dynamicRouteByApolloListener (){
        try {
            ConfigFile config = ConfigService.getConfigFile("RouteEditor" , ConfigFileFormat.JSON);
            log.info(config.getContent());
            changeRoute(config.getContent());
            config.addChangeListener((ConfigFileChangeEvent changeEvent) -> {
                log.info("Changes for namespace " + changeEvent.getNamespace());
                log.info(String.format("Found change - oldValue: %s, newValue: %s, changeType: %s", changeEvent.getOldValue(), changeEvent.getNewValue(), changeEvent.getChangeType()));
                changeRoute(changeEvent.getNewValue());
            });
        } catch (Exception e) {
            log.error(e.getMessage() , e);
        }
    }

    private void changeRoute(String value){
        List<RouteDefinition> routeDefinitions = JsonUtils.jsonToArray(value , RouteDefinition.class);
        Set<String> newRouteIds = new HashSet<>();
        for(RouteDefinition routeDefinition : routeDefinitions){
            newRouteIds.add(routeDefinition.getId());
            ROUTE_IDS.add(routeDefinition.getId());
            dynamicRouteService.save(routeDefinition);
        }
        for(String routeId : ROUTE_IDS){
            if(!newRouteIds.contains(routeId)){
                dynamicRouteService.delete(routeId);
            }
        }
        ROUTE_IDS = newRouteIds;
    }

}
