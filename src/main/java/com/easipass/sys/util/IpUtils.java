package com.easipass.sys.util;

import com.easipass.commoncore.util.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * @Description: 获取IP工具类
 * @author: mlzhang
 * @date: 2016/10/17 16:56
 * @version: V1.0
 */
public class IpUtils {
    private static final Log log = LogFactory.getLog(com.easipass.commoncore.util.IpUtils.class);

    public static String getIp(HttpServletRequest request) {
        String ip;
        String remoteAddr=null;
        try{
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            if(null != ip && ip.length() > 20){
                ip = ip.substring(0 , 20);
            }
            if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
                //多次反向代理后会有多个ip值，第一个ip才是真实ip
                int index = ip.indexOf(",");
                if (index != -1) {
                    return ip.substring(0, index);
                } else {
                    return ip;
                }
            }

            ip = request.getHeader("X-Forwarded-For");
            if(null != ip && ip.length() > 20){
                ip = ip.substring(0 , 20);
            }
            if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
                //多次反向代理后会有多个ip值，第一个ip才是真实ip
                int index = ip.indexOf(",");
                if (index != -1) {
                    return ip.substring(0, index);
                } else {
                    return ip;
                }
            }
            ip = request.getHeader("X-Real-IP");
            if(null != ip && ip.length() > 20){
                ip = ip.substring(0 , 20);
            }
            if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
                return ip;
            }
            remoteAddr = request.getRemoteAddr();
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        return remoteAddr;
    }

    public static String getIp(ServerHttpRequest request) {
        String userIp = "";
        try{
            if(null != request.getHeaders().get("HTTP_X_FORWARDED_FOR")){
                userIp = Collections.unmodifiableList(request.getHeaders().get("HTTP_X_FORWARDED_FOR")).get(0);
                log.info("HTTP_X_FORWARDED_FOR:" + userIp);
                if (StringUtils.isNotEmpty(userIp) && !"unKnown".equalsIgnoreCase(userIp)) {
                    if(userIp.length() > 20){userIp = userIp.substring(0 , 20);}
                    //多次反向代理后会有多个ip值，第一个ip才是真实ip
                    int index = userIp.indexOf(",");
                    if (index != -1) {userIp = userIp.substring(0, index);}
                }
            }
            if(StringUtils.isEmpty(userIp)){
                if(null != request.getHeaders().get("X-Forwarded-For")){
                    userIp = Collections.unmodifiableList(request.getHeaders().get("X-Forwarded-For")).get(0);
                    log.info("X-Forwarded-For:" + userIp);
                    if (StringUtils.isNotEmpty(userIp) && !"unKnown".equalsIgnoreCase(userIp)) {
                        if(userIp.length() > 20){userIp = userIp.substring(0 , 20);}
                        int index = userIp.indexOf(",");
                        if (index != -1) {userIp = userIp.substring(0, index);}
                    }
                }
            }
            if(StringUtils.isEmpty(userIp)){
                if(null != request.getHeaders().get("X-Real-IP")){
                    userIp = Collections.unmodifiableList(request.getHeaders().get("X-Real-IP")).get(0);
                    log.info("X-Real-IP:" + userIp);
                    if (StringUtils.isNotEmpty(userIp) && !"unKnown".equalsIgnoreCase(userIp)) {
                        if(userIp.length() > 20){userIp = userIp.substring(0 , 20);}
                    }
                }
            }
            if(StringUtils.isEmpty(userIp)){
                userIp = request.getRemoteAddress().getAddress().getHostAddress();
                log.info("RemoteAddr:" + userIp);
            }

        }catch (Exception e){
            log.error(e.getMessage() , e);
        }
        return  userIp;
    }

}
