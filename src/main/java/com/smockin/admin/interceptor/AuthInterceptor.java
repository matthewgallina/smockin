package com.smockin.admin.interceptor;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.service.AuthService;
import com.smockin.admin.service.AuthServiceImpl;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "auth")
public class AuthInterceptor extends HandlerInterceptorAdapter {

    private final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private AuthService authService;

    // Injected from application.yaml
    private final Map<String, List<String>> exclusions = new HashMap<>();

    /*
        For more info:
            https://www.tuturself.com/posts/view?menuId=3&postId=1071
            http://www.baeldung.com/spring-mvc-handlerinterceptor
    */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("Request received...");

        if (!UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())) {
            return true;
        }

        debugRequest(request);

        final boolean isExcluded = exclusions.entrySet()
            .stream()
            .anyMatch(e ->
                    matchExclusionUrl(e.getKey(), request.getRequestURI())
                        && e.getValue().stream().anyMatch(m -> m.equalsIgnoreCase(request.getMethod()))
            );

        if (isExcluded) {
            return true;
        }

        final String bearerToken = request.getHeader(GeneralUtils.OAUTH_HEADER_NAME);
        final String token = GeneralUtils.extractOAuthToken(bearerToken);

        // Check token exists in DB
        smockinUserService.lookUpToken(token);

        // Check token is valid
        authService.verifyToken(token);

        return true;
    }

    private boolean matchExclusionUrl(final String exclusionKey, final String inboundUrl) {

        final int wildCardPos = exclusionKey.indexOf("*");

        if (wildCardPos > -1) {
            return inboundUrl.startsWith(exclusionKey.substring(0, wildCardPos));
        }

        return exclusionKey.equalsIgnoreCase(inboundUrl);
    }

    public Map<String, List<String>> getExclusions() {
        return exclusions;
    }

    @PostConstruct
    public void after() {

        if (logger.isDebugEnabled()) {
            logger.debug("Current Exclusions:");
            exclusions.entrySet().forEach(e -> logger.debug("key: " + e.getKey() + ", value: " + e.getValue()));
        }

    }

    private void debugRequest(final HttpServletRequest request) {

        if (logger.isDebugEnabled()) {
            logger.debug("Request URI: " + request.getRequestURI());
            logger.debug("Request Method: " + request.getMethod());
        }

    }

}
