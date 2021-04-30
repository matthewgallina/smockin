package com.smockin.admin.websocket;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import javax.servlet.ServletContext;
import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final String URL = "/liveLoggingFeed/*";

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private LiveLoggingHandler mockLogFeedHandler;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private SmockinUserService smockinUserService;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(((TextWebSocketHandler) mockLogFeedHandler), URL)
                .addInterceptors(userInterceptor())
                .setHandshakeHandler(handshakeHandler());
    }

    private DefaultHandshakeHandler handshakeHandler() {
        return new DefaultHandshakeHandler(
                new JettyRequestUpgradeStrategy(
                        new WebSocketServerFactory(servletContext,
                                new WebSocketPolicy(WebSocketBehavior.SERVER))));
    }

    public HandshakeInterceptor userInterceptor() {

        return new HandshakeInterceptor() {

            public boolean beforeHandshake(final ServerHttpRequest request,
                                           final ServerHttpResponse response,
                                           final WebSocketHandler wsHandler,
                                           final Map<String, Object> attributes) throws Exception {

                if (!UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())) {
                    return true;
                }

                final String path = request.getURI().getPath();
                final int position = path.lastIndexOf(GeneralUtils.URL_PATH_SEPARATOR);

                if (position == -1) {
                    return false;
                }

                final String token = path.substring(position + 1);

                if (token == null) {
                    return false;
                }

                final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentUser(token);

                attributes.put(LiveLoggingHandler.WS_CONNECTED_USER_ROLE, smockinUser.getRole());
                attributes.put(LiveLoggingHandler.WS_CONNECTED_USER_CTX_PATH, smockinUser.getCtxPath());

                return true;
            }

            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
            }
        };
    }

}
