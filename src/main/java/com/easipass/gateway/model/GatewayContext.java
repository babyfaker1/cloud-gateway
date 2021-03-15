package com.easipass.gateway.model;


import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Data
public class GatewayContext {

    public static final String CACHE_GATEWAY_CONTEXT = "cacheGatewayContext";

    /**
     * cache request method
     */
    private String requestMethod;

    /**
     * cache request path
     */
    private String path;

    /**
     * cache json body
     */
    private String cacheBody;
    /**
     * cache formdata
     */
    private MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

    private String requestParames;

    private String requestJsonParams;

    private String headers;

    private String fullUrl;
}
