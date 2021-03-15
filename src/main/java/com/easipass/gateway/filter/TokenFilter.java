package com.easipass.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.easipass.commoncore.constant.SysErrorCode;
import com.easipass.commoncore.model.EpOauthModuleReturnInfo;
import com.easipass.commoncore.model.EpOauthModuleUserInfo;
import com.easipass.commoncore.util.JsonUtils;
import com.easipass.commoncore.util.StringUtils;
import com.easipass.gateway.config.GatewayOauthAppConf;
import com.easipass.gateway.util.HandleExceptionUtils;
import com.easipass.gateway.util.HttpUtils;
import com.easipass.gateway.util.MatchUtils;
import com.easipass.oauthmodule.config.EpOauthModuleProperties;
import com.easipass.oauthmodule.service.EpOauthModuleAppService;
import com.easipass.sys.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;

@Component
@Slf4j
public class TokenFilter implements GlobalFilter, Ordered {

    @Autowired
    EpOauthModuleAppService epOauthModuleAppService;

    @Value("${easipass.oauth.config.oauthUrl}")
    private String oauthUrl;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            log.info("-------- TokenFilter --------");
            URI uri = exchange.getRequest().getURI();
            String path = uri.getPath();
            log.info(path);
            JSONObject relations = GatewayOauthAppConf.getRelations();
            JSONObject appInfo = relations.getJSONObject(uri.getPath().split("/")[1]);
            // 应用在Apollo上有配置，过token拦截器
            if (!path.contains("/epoa/") && null != appInfo) {
                String whiteListStr = (String) appInfo.get("whiteList");//白名单（不用验证）
                String validateListStr = (String) appInfo.get("validateList");//验证名单（需要验证）
                if (!StringUtils.isEmpty(whiteListStr)) {
                    String[] whiteList = whiteListStr.split(",");
                    for(String white : whiteList){
                        if(MatchUtils.match(path, white)){
                            // 匹配，向后发送
                            return chain.filter(exchange);
                        }
                    }
                }else if(!StringUtils.isEmpty(validateListStr)){
                    String[] validateList = validateListStr.split(",");
                    Boolean validateFlag = false;
                    for(String validate : validateList){
                        if(MatchUtils.match(path, validate)){
                            validateFlag = true;
                        }
                    }
                    if(!validateFlag){
                        // 一个都不匹配，向后发送
                        return chain.filter(exchange);
                    }
                }
                // 请求准备
                EpOauthModuleProperties epOauthModuleProperties = new EpOauthModuleProperties();
                epOauthModuleProperties.setOauthUrl(oauthUrl);
                epOauthModuleProperties.setClientId(StringUtils.valueOf(appInfo.getString("clientId")));
                epOauthModuleProperties.setAppCode(StringUtils.valueOf(appInfo.getString("appCode")));
                epOauthModuleAppService.setEpOauthModuleProperties(epOauthModuleProperties);
                String userIp = IpUtils.getIp(exchange.getRequest());

                // 整理token
                String userAuthCode = HttpUtils.getParamByExchange(exchange , new String[]{"userAuthCode"});
                String refreshToken = null;
                if (!StringUtils.isEmpty(userAuthCode)) {//大客户授权码换RT
                    EpOauthModuleReturnInfo epOauthModuleReturnInfo = epOauthModuleAppService.authedCompanyRtGet(userAuthCode , userIp);
                    refreshToken = epOauthModuleReturnInfo.getRefreshToken();
                }

                if(StringUtils.isEmpty(refreshToken)){
                    refreshToken = HttpUtils.getParamByExchange(exchange , new String[]{"refresh_token" , "epToken"});
                }

                // 检测到有RT
                if (StringUtils.isEmpty(refreshToken)) {
                    return HandleExceptionUtils.handleErrJsonResponse(SysErrorCode.ERROR_CODE301, exchange);
                }
                EpOauthModuleReturnInfo epOauthModuleReturnInfo = epOauthModuleAppService.accesstokenGet(refreshToken , userIp);
                if ("RefreshTokenNoFind".equals(epOauthModuleReturnInfo.getErrorCode())) {
                    return HandleExceptionUtils.handleErrJsonResponse(SysErrorCode.ERROR_CODE302, exchange);
                }
                if (!"T".equals(epOauthModuleReturnInfo.getFlag())) {
                    return HandleExceptionUtils.handleErrJsonResponse(SysErrorCode.ERROR_CODE305.getErrorCode(), epOauthModuleReturnInfo.getErrorInfo(), exchange);
                }
                EpOauthModuleUserInfo epOauthModuleUserInfo = epOauthModuleAppService.userinfoByATGet(epOauthModuleReturnInfo.getAccessToken());
                if (!"T".equals(epOauthModuleUserInfo.getFlag())) {
                    return HandleExceptionUtils.handleErrJsonResponse(SysErrorCode.ERROR_CODE305.getErrorCode(), epOauthModuleUserInfo.getErrorInfo(), exchange);
                }

                if(appInfo.getBooleanValue("permitSwitch") && !MatchUtils.match(path, epOauthModuleUserInfo)){
                    return HandleExceptionUtils.handleErrJsonResponse(SysErrorCode.ERROR_CODE304, exchange);
                }
                // 正确，向后发送，向headers中放文件，记得build
                ServerHttpRequest request = exchange.getRequest().mutate().header("EpGatewayOauthUserInfo", URLEncoder.encode(JsonUtils.beanToJson(epOauthModuleUserInfo) , "UTF-8")).build();
                return chain.filter(exchange.mutate().request(request).build());

            }
        } catch (Exception e) {
            log.error(e.getMessage() , e);
            return HandleExceptionUtils.handleErrJsonResponse (SysErrorCode.ERROR_CODE901, exchange);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
