package com.telegram.dto.response;

public record WebSocketEvent<T>(
        String type,
        T payload
) {
    public static <T> WebSocketEvent<T> of(String type, T payload) {
        return new WebSocketEvent<>(type, payload);
    }
}