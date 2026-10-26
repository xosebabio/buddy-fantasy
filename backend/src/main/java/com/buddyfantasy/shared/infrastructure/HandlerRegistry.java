package com.buddyfantasy.shared.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.GenericTypeResolver;

/** Maps a message type (command or query) to the single handler declared for it. */
final class HandlerRegistry<H> {

    private final Map<Class<?>, H> handlers = new HashMap<>();

    HandlerRegistry(List<H> candidates, Class<?> handlerInterface) {
        for (H handler : candidates) {
            Class<?>[] args = GenericTypeResolver.resolveTypeArguments(AopUtils.getTargetClass(handler), handlerInterface);
            if (args == null) {
                throw new IllegalStateException("Cannot resolve message type of " + handler.getClass().getName());
            }
            H previous = handlers.put(args[0], handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate handler for " + args[0].getName());
            }
        }
    }

    H find(Class<?> messageType) {
        H handler = handlers.get(messageType);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for " + messageType.getName());
        }
        return handler;
    }
}
