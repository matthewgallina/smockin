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

        if (logger.isDebugEnabled()) {
            logger.debug("Request URI: " + request.getRequestURI());
            logger.debug("Request Method: " + request.getMethod());
        }

        final boolean isExcluded = exclusions.entrySet()
            .stream()
            .anyMatch(e ->
                e.getKey().equalsIgnoreCase(request.getRequestURI())
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

    public Map<String, List<String>> getExclusions() {
        return exclusions;
    }
}
