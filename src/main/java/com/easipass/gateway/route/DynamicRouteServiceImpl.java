package com.easipass.gateway.route;

import com.easipass.commoncore.util.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class DynamicRouteServiceImpl implements ApplicationEventPublisherAware {

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    @Autowired
    InMemoryRouteDefinitionRepository inMemoryRouteDefinitionRepository;

    private ApplicationEventPublisher publisher;


    /**
     * 更新路由
     * @param definition
     * @return
     */
    public String save(RouteDefinition definition) {
        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return "success";
    }

    /**
     * 删除路由
     * @param id
     * @return
     */
    public String delete(String id){
        try{
            Map<String, RouteDefinition> routes = (Map<String, RouteDefinition>) ReflectUtils.getPrivateParam(inMemoryRouteDefinitionRepository , "routes");
            if (routes.containsKey(id)) {
                routes.remove(id);
            }
            return "success";
        }catch (Exception e){
            log.error(e.getMessage() , e);
            return "error";
        }
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

}
