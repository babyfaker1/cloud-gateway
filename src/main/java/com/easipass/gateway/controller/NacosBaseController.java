package com.easipass.gateway.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.easipass.commoncore.util.HttpUtils;
import com.easipass.commoncore.util.JsonUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

public class NacosBaseController {

    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String nacosServerAddr;

    public JSONObject getServiceList(String pageNo , String pageSize){
        String result = HttpUtils.getInstance().sendHttpGet("http://" + nacosServerAddr + "/nacos/v1/ns/service/list?pageNo=" + pageNo + "&pageSize=" + pageSize);
        JSONObject serviceList = JsonUtils.jsonToBean(result);
        return serviceList;
    }

    public JSONObject getServiceInfo(String serviceName){
        String result = HttpUtils.getInstance().sendHttpGet("http://" + nacosServerAddr + "/nacos/v1/ns/service?serviceName=" + serviceName);
        JSONObject service = JsonUtils.jsonToBean(result);
        return service;
    }

    public JSONObject getAllInstances(String serviceName , String healthyOnly){
        Map requestMap = new HashMap<>();
        requestMap.put("serviceName" , serviceName);
        if(null != healthyOnly){
            requestMap.put("healthyOnly" , healthyOnly);
        }
        String result = HttpUtils.getInstance().sendHttpGet("http://" + nacosServerAddr + "/nacos/v1/ns/instance/list" , requestMap);
        JSONObject instances = JsonUtils.jsonToBean(result);
        return instances;
    }

    public void putInstance(String serviceName , String ip , String port , String metadata , String enabled , String weight){
        Map requestMap = new HashMap<>();
        requestMap.put("serviceName" , serviceName);
        requestMap.put("ip" , ip);
        requestMap.put("port" , port);
        if(null != metadata){
            requestMap.put("metadata" , metadata);
        }
        if(null != enabled){
            requestMap.put("enabled" , enabled);
        }
        if(null != weight){
            requestMap.put("weight" , weight);
        }
        HttpUtils.getInstance().sendHttpPut("http://" + nacosServerAddr + "/nacos/v1/ns/instance" , requestMap);
    }

    public JSONObject getCatalogServices(String pageNo , String pageSize , String keyword){
        String result = HttpUtils.getInstance().sendHttpGet("http://" + nacosServerAddr + "/nacos/v1/ns/catalog/services?withInstances=false&pageNo=" + pageNo + "&pageSize=" + pageSize + "&keyword=" + keyword);
        JSONObject jsonObject = JsonUtils.jsonToBean(result);
        return jsonObject;
    }

    public JSONObject getCatalogService(String serviceName){
        String result = HttpUtils.getInstance().sendHttpGet("http://" + nacosServerAddr + "/nacos/v1/ns/catalog/service?serviceName=" + serviceName);
        JSONObject jsonObject = JsonUtils.jsonToBean(result);
        return jsonObject;
    }

    public JSONObject getCatalogInstances(String pageNo , String pageSize , String serviceName){
        String result = HttpUtils.getInstance().sendHttpGet("http://" + nacosServerAddr + "/nacos/v1/ns/catalog/instances?clusterName=DEFAULT&pageNo=" + pageNo + "&pageSize=" + pageSize + "&serviceName=" + serviceName);
        JSONObject jsonObject = JsonUtils.jsonToBean(result);
        return jsonObject;
    }
}
