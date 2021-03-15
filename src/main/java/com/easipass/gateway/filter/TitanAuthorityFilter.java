package com.easipass.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.easipass.commoncore.constant.SysErrorCode;
import com.easipass.commoncore.model.ApiResult;
import com.easipass.commoncore.model.EpOauthModuleReturnInfo;
import com.easipass.commoncore.util.HttpUtils;
import com.easipass.commoncore.util.JsonUtils;
import com.easipass.commoncore.util.StringUtils;
import com.easipass.gateway.config.TitanOauthAppConf;
import com.easipass.gateway.util.HandleExceptionUtils;
import com.easipass.gateway.util.MatchUtils;
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


/**
 * @Author:JiTao Created by dell on 2020/5/18.
 * <p>
 * 鉴权过滤器
 */
@Component
@Slf4j
public class TitanAuthorityFilter implements GlobalFilter, Ordered {

    @Autowired
    EpOauthModuleAppService epOauthModuleAppService;


    @Value("${easipass.auth.url:http://192.168.118.129:9087}")
    private String verifyUrl;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("-----------VerifyRoleFilter-------------");
        URI uri = exchange.getRequest().getURI();
        String path = uri.getPath();
        String appCode = uri.getPath().split("/")[1];
        log.info("path:" + path);
        //设置APPCODE校验名单

        //1.校验当前路径是否通过此过滤器
        if (!verifyPassPath(exchange)) {
            return chain.filter(exchange);
        }

        // 整理token
        String userIp = IpUtils.getIp(exchange.getRequest());
        String userAuthCode = com.easipass.gateway.util.HttpUtils.getParamByExchange(exchange , new String[]{"userAuthCode"});
        String token = null;
        if (!StringUtils.isEmpty(userAuthCode)) {
            EpOauthModuleReturnInfo epOauthModuleReturnInfo = epOauthModuleAppService.authedCompanyRtGet(userAuthCode , userIp);
            token = epOauthModuleReturnInfo.getRefreshToken();
        }

        if(StringUtils.isEmpty(token)){
            token = com.easipass.gateway.util.HttpUtils.getParamByExchange(exchange , new String[]{"refresh_token" , "epToken"});
        }

        // 检测到有RT
        if (StringUtils.isEmpty(token)) {
            return HandleExceptionUtils.handleErrJsonResponse(SysErrorCode.ERROR_CODE301, exchange);
        }

        if (token == null) {
            return HandleExceptionUtils.handleErrJsonResponse(SysErrorCode.ERROR_CODE301, exchange);
        }

        ApiResult apiResult = verify(token, appCode, path);
        if ("F".equals(apiResult.getFlag())) {
            return HandleExceptionUtils.handleErrJsonResponse(apiResult.getErrorCode(),apiResult.getErrorInfo(), exchange);
        } else {
            String userInfo = apiResult.getData().toString();
            ServerHttpRequest request = null;
            try {
                request = exchange.getRequest().mutate().header("userInfo", URLEncoder.encode(userInfo, "UTF-8")).build();
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage(), e);
                return HandleExceptionUtils.handleErrJsonResponse(SysErrorCode.ERROR_CODE901, exchange);
            }

            return chain.filter(exchange.mutate().request(request).build());
        }
    }

    private boolean verifyPassPath(ServerWebExchange exchange) {
        URI uri = exchange.getRequest().getURI();
        String path = uri.getPath();
        String appCode = path.split("/")[1];
        //如果apollo中没有配置，不经过过滤器
        JSONObject relations = TitanOauthAppConf.getRelations();
        if (relations == null) {
            return false;
        }
        JSONObject appInfo = relations.getJSONObject(appCode);
        //1.appCode 没有配置 不经过过滤器
        if (appInfo == null) {
            return false;
        }

        String whiteListStr = (String) appInfo.get("whiteList");
        //2.已配置的appcode 如果在白名单中就不经过过滤器
        if (!StringUtils.isEmpty(whiteListStr)) {
            String[] whiteList = whiteListStr.split(",");
            for (String white : whiteList) {
                if (MatchUtils.match(path, white)) {
                    return false;
                }
            }
        }

        return true;
    }


    @Override
    public int getOrder() {
        return -200;
    }

    private ApiResult verify(String token, String appCode, String path) {

        String url = verifyUrl + "canAuth";
        Map<String, String> paraMap = new HashMap<>();
        paraMap.put("token", token);
        paraMap.put("appCode", appCode);
        paraMap.put("path", path);
        String s = HttpUtils.getInstance().sendHttpPost(url, paraMap);
        return JsonUtils.jsonToBean(s, ApiResult.class);
    }
}
