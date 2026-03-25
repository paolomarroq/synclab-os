package com.synclabos.producerconsumer;

@FunctionalInterface
public interface StateListener {
    void onStateChange(ProducerConsumerState state);
}
