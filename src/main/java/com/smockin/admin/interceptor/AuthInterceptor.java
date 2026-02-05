package com.smockin.admin.interceptor;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.service.AuthService;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.utils.GeneralUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);


    private final SmockinUserService smockinUserService;

   
    private final AuthService authService;

    // Resorted to using json, in place of defining the list structure in yaml, due to a bug introduced
    // in Spring Boot v2 where forward slashes (& other characters) seem to be stripped out when injected in.
    @Value("${smockin.auth.exclusions:#{null}}")
    private String exclusionsJson;

    private Map<String, List<String>> exclusions;

    /*
        For more info:
            https://www.tuturself.com/posts/view?menuId=3&postId=1071
            http://www.baeldung.com/spring-mvc-handlerinterceptor
    */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
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

    boolean matchExclusionUrl(final String exclusionKey, final String inboundUrl) {

        final int wildCardFilePos = exclusionKey.indexOf("*.");
        final int wildCardPathPos = exclusionKey.indexOf("*");

        if (wildCardFilePos > -1) {
            return inboundUrl.endsWith(exclusionKey.substring(wildCardFilePos + 1));
        } else if (wildCardPathPos > -1) {
            return inboundUrl.startsWith(exclusionKey.substring(0, wildCardPathPos));
        }

        return exclusionKey.equalsIgnoreCase(inboundUrl);
    }

    @PostConstruct
    public void after() {

        final Map<String, List<String>> exclusionsMap =
                (StringUtils.isNotBlank(exclusionsJson))
                        ? GeneralUtils.deserialiseJson(exclusionsJson)
                        : new HashMap<>();

        exclusions = Collections.unmodifiableMap(exclusionsMap);

        if (logger.isDebugEnabled()) {
            logger.debug("Current Exclusions:");
            exclusions.forEach((key, value) -> logger.debug("exclusion path: {} , method: {}" ,key, value));
        }

    }

    private void debugRequest(final HttpServletRequest request) {

        if (logger.isDebugEnabled()) {
            logger.debug("Request URI: {}" , request.getRequestURI());
            logger.debug("Request Method: {}" , request.getMethod());
        }

    }

}
