package com.easipass.gateway.controller;

import com.easipass.commoncore.model.ApiResult;
import com.easipass.commoncore.util.HttpUtils;
import com.easipass.commoncore.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("counter")
public class CounterController {

    static String oauthUrl;

    static String applicationName;

    @Value("${easipass.oauth.config.oauthUrl}")
    public void setOauthUrl(String oauthUrl) {
        CounterController.oauthUrl = oauthUrl;
    }

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        CounterController.applicationName = applicationName;
    }

    @PostMapping("reset/{predicate}/{countGroup}")
    public ApiResult reset(@PathVariable String predicate , @PathVariable String countGroup){
        return oauthRedisSet(predicate + ":" + countGroup , "0");
    }


    public static ApiResult oauthRedisGet(String key){
        Map requestMap = new HashMap();
        requestMap.put("who" , applicationName);
        requestMap.put("key" , key);
        return JsonUtils.jsonToBean(HttpUtils.getInstance().sendHttpGet(oauthUrl + "/redis/get" , requestMap) , ApiResult.class);
    }

    public static ApiResult oauthRedisSet(String key , String value){
        Map requestMap = new HashMap();
        requestMap.put("who" , applicationName);
        requestMap.put("key" , key);
        requestMap.put("value" , value);
        return JsonUtils.jsonToBean(HttpUtils.getInstance().sendHttpPost(oauthUrl + "/redis/set" , requestMap) , ApiResult.class);
    }

    public static ApiResult oauthRedisSetEx(String key , String value , int second){
        Map requestMap = new HashMap();
        requestMap.put("who" , applicationName);
        requestMap.put("key" , key);
        requestMap.put("value" , value);
        requestMap.put("second" , String.valueOf(second));
        return JsonUtils.jsonToBean(HttpUtils.getInstance().sendHttpPost(oauthUrl + "/redis/setEx" , requestMap) , ApiResult.class);
    }

}
