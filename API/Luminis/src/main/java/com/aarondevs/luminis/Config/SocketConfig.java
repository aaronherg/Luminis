package com.aarondevs.luminis.Config;

import com.aarondevs.luminis.Services.Mensajeria.LuminisSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class SocketConfig implements WebSocketConfigurer {
    private final LuminisSocketHandler handler;
    private final int maximoKb;

    public SocketConfig(LuminisSocketHandler handler, @Value("${app.luminis.socket.max-kb:1024}") int maximoKb) {
        this.handler = handler;
        this.maximoKb = maximoKb;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/socket").setAllowedOriginPatterns("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean contenedor = new ServletServerContainerFactoryBean();
        contenedor.setMaxTextMessageBufferSize(maximoKb * 1024 + 4096);
        contenedor.setMaxBinaryMessageBufferSize(64 * 1024);
        contenedor.setMaxSessionIdleTimeout(120_000L);
        return contenedor;
    }
}
