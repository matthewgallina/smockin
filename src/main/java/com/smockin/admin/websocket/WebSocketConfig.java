package com.smockin.admin.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import javax.servlet.ServletContext;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private MockLogFeedHandler mockLogFeedHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(((TextWebSocketHandler)mockLogFeedHandler), "/httpMockLogFeed")
                .setAllowedOrigins("*") // TODO MG temp for DEV remove this!!
                .setHandshakeHandler(handshakeHandler());
    }

    @Bean
    public ObjectMapper jsonMapper() {
        return new ObjectMapper();
    }

    private DefaultHandshakeHandler handshakeHandler() {
        return new DefaultHandshakeHandler(
                new JettyRequestUpgradeStrategy(
                        new WebSocketServerFactory(servletContext,
                                new WebSocketPolicy(WebSocketBehavior.SERVER))));
    }

}
