package com.easipass.gateway.util;

import com.easipass.commoncore.model.EpOauthModuleUserInfo;
import com.easipass.commoncore.util.StringUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

public class MatchUtils {

    private static PathMatcher urlMatcher = new AntPathMatcher();

    public static boolean match (String reqUrl, String matchStr) {
        String serviceName = reqUrl.split("/")[1];
        boolean matched = false;
        if (!StringUtils.isEmpty(matchStr)) {
            String[] matchArr = matchStr.split(",");
            for (int i = 0; i < matchArr.length; i++) {
                String matchUrl = "/" + serviceName + matchArr[i];
                if (urlMatcher.match(matchUrl, reqUrl)) {
                    matched = true;
                    break;
                }
            }
        }
        return matched;
    }

    public static boolean match(String reqUrl , EpOauthModuleUserInfo oui) {
        boolean matched = false;
        if (null != oui  && null != oui.getInfo() && null != oui.getInfo().getPermits() && oui.getInfo().getPermits().size() > 0) {
            for (EpOauthModuleUserInfo.UserInfo.UmPermission key : oui.getInfo().getPermits()) {
                String url = key.getUrl();
                if (urlMatcher.match(url, reqUrl)) {
                    matched = true;
                    break;
                }
            }
        }
        return matched;
    }
}
