package com.easipass.gateway.entity;

import com.easipass.commoncore.util.JsonUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class RecordEntity implements Serializable {

    private String recordId;

    private String section;

    private String topicProject;

    private String requestMethod;

    private String host;

    private Integer port;

    private String path;

    private String requestHeaders;

    private String cookies;

    private String requestParams;

    private String requestBody;

    private String response;

    //true 成功 false 失败
    private Boolean responseFlag;

    //根据时间排序
    private Long timeOrder;

    private Date timestamp;

    private String fullUrl;

    public String toString() {
        return JsonUtils.beanToJsonWithNullValue(this);
    }


//    private Long proceedMills;
//    private String sessionId;
//    private String businessFlag;
//    private String afterReturningResult;
//    private String exceptionMsg;
//    private String stackTraceMsg;
//    private String userIp;
//    private String serverIp;
//    private String userAgent;



}
