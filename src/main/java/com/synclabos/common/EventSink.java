package com.synclabos.common;

@FunctionalInterface
public interface EventSink {
    void log(String message);
}
