package com.aarondevs.luminis.Services.Mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class LuminisSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(LuminisSocketHandler.class);

    private final SocketHub hub;

    public LuminisSocketHandler(SocketHub hub) {
        this.hub = hub;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        hub.abrir(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        hub.recibir(session, message.getPayload());
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        hub.pong(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Error de transporte en la sesión {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        hub.cerrar(session);
    }
}
