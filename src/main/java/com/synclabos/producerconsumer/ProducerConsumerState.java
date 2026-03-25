package com.synclabos.producerconsumer;

import java.util.List;
import java.util.Map;

public record ProducerConsumerState(
    List<String> slots,
    int size,
    int capacity,
    String producerState,
    Map<String, String> consumerStates,
    Map<String, Integer> consumerSums,
    String criticalSectionOwner
) {
}
