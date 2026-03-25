package com.synclabos.producerconsumer;

import com.synclabos.common.EventSink;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumerSimulation {
    private final EventSink eventSink;
    private final StateListener stateListener;
    private final int capacity;
    private final int consumerGroups;
    private final SharedBuffer buffer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean producerDone = new AtomicBoolean(false);
    private final Object pauseMonitor = new Object();

    private Thread producerThread;
    private final List<Thread> consumerThreads = new CopyOnWriteArrayList<>();
    private final Map<String, String> consumerStates = new HashMap<>();
    private final Map<String, Integer> consumerSums = new HashMap<>();
    private volatile String producerState = "IDLE";
    private volatile String criticalSectionOwner = "NINGUNA";

    public ProducerConsumerSimulation(int capacity, int consumerGroups, EventSink eventSink, StateListener stateListener) {
        if (consumerGroups <= 0) {
            throw new IllegalArgumentException("consumerGroups debe ser mayor que cero.");
        }
        this.capacity = capacity;
        this.consumerGroups = consumerGroups;
        this.eventSink = eventSink;
        this.stateListener = stateListener;
        this.buffer = new SharedBuffer(capacity);
        initializeConsumersMetadata();
        publishState();
    }

    public synchronized void start(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath");
        List<Integer> numbers = readNumbers(filePath);
        start(numbers, "Archivo cargado: " + filePath);
    }

    public synchronized void start(List<Integer> numbers, String sourceLabel) {
        Objects.requireNonNull(numbers, "numbers");
        stop();
        resetMetadata();
        running.set(true);
        producerDone.set(false);
        paused.set(false);

        producerThread = new Thread(() -> runProducer(new ArrayList<>(numbers)), "Productor");
        producerThread.start();
        startConsumers();
        eventSink.log("Simulación iniciada. " + sourceLabel);
    }

    public synchronized void pause() {
        if (!running.get()) {
            return;
        }
        paused.set(true);
        producerState = "PAUSADO";
        consumerStates.replaceAll((k, v) -> "PAUSADO");
        eventSink.log("Simulación pausada.");
        publishState();
    }

    public synchronized void resume() {
        if (!running.get()) {
            return;
        }
        paused.set(false);
        synchronized (pauseMonitor) {
            pauseMonitor.notifyAll();
        }
        eventSink.log("Simulación reanudada.");
    }

    public synchronized void stop() {
        if (!running.get() && producerThread == null) {
            return;
        }
        running.set(false);
        paused.set(false);
        synchronized (pauseMonitor) {
            pauseMonitor.notifyAll();
        }
        buffer.wakeAll();
        joinThread(producerThread);
        producerThread = null;
        for (Thread consumerThread : consumerThreads) {
            joinThread(consumerThread);
        }
        consumerThreads.clear();
        producerState = "DETENIDO";
        eventSink.log("Simulación detenida.");
        publishState();
    }

    public synchronized void reset() {
        stop();
        buffer.clear();
        resetMetadata();
        producerState = "IDLE";
        criticalSectionOwner = "NINGUNA";
        publishState();
    }

    private void runProducer(List<Integer> numbers) {
        producerState = "ACTIVO";
        publishState();
        try {
            for (int number : numbers) {
                if (!running.get()) {
                    break;
                }
                awaitIfPaused();
                NumberItem item = NumberItem.fromValue(number);
                producerState = "INSERTANDO";
                publishState();
                if (!buffer.put(item, this::setCriticalOwner, this::setProducerBlockedFull, running)) {
                    break;
                }
                eventSink.log("Productor insertó " + item.value() + " [" + item.type().label() + "]");
                producerState = "ACTIVO";
                publishState();
                sleep(450);
            }
        } finally {
            producerDone.set(true);
            producerState = "FINALIZADO";
            buffer.wakeAll();
            publishState();
            checkFinished();
        }
    }

    private void startConsumers() {
        int index = 1;
        for (int group = 0; group < consumerGroups; group++) {
            index = createConsumer(index, NumberType.EVEN, "PARES");
            index = createConsumer(index, NumberType.ODD, "IMPARES");
            index = createConsumer(index, NumberType.PRIME, "PRIMOS");
        }
    }

    private int createConsumer(int id, NumberType type, String label) {
        String consumerId = "C" + id + "-" + label;
        consumerStates.put(consumerId, "LISTO");
        consumerSums.put(consumerId, 0);
        Thread thread = new Thread(() -> runConsumer(consumerId, type), consumerId);
        consumerThreads.add(thread);
        thread.start();
        return id + 1;
    }

    private void runConsumer(String consumerId, NumberType type) {
        while (running.get()) {
            awaitIfPaused();
            consumerStates.put(consumerId, "ESPERANDO");
            publishState();
            NumberItem item = buffer.takeOfType(
                type,
                this::setCriticalOwner,
                () -> setConsumerBlocked(consumerId),
                producerDone,
                running
            );
            if (item == null) {
                break;
            }
            consumerStates.put(consumerId, "EXTRAYENDO");
            int newSum = consumerSums.get(consumerId) + item.value();
            consumerSums.put(consumerId, newSum);
            eventSink.log(consumerId + " consumió " + item.value() + " -> suma=" + newSum);
            publishState();
            sleep(800);
        }
        consumerStates.put(consumerId, "FINALIZADO");
        publishState();
        checkFinished();
    }

    private void checkFinished() {
        if (!producerDone.get()) {
            return;
        }
        for (Thread thread : consumerThreads) {
            if (thread.isAlive()) {
                return;
            }
        }
        running.set(false);
        eventSink.log("Simulación finalizada (productor y consumidores completaron su trabajo).");
    }

    private void setCriticalOwner(String owner) {
        this.criticalSectionOwner = owner;
        publishState();
    }

    private void setProducerBlockedFull() {
        producerState = "BLOQUEADO_BUFFER_LLENO";
        publishState();
    }

    private void setConsumerBlocked(String consumerId) {
        if (buffer.isEmpty()) {
            consumerStates.put(consumerId, "BLOQUEADO_BUFFER_VACIO");
        } else {
            consumerStates.put(consumerId, "BLOQUEADO_POR_TIPO");
        }
        publishState();
    }

    private void awaitIfPaused() {
        while (paused.get() && running.get()) {
            synchronized (pauseMonitor) {
                try {
                    pauseMonitor.wait(120);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void publishState() {
        stateListener.onStateChange(
            new ProducerConsumerState(
                buffer.slotView(),
                buffer.size(),
                capacity,
                producerState,
                new HashMap<>(consumerStates),
                new HashMap<>(consumerSums),
                criticalSectionOwner
            )
        );
    }

    private void initializeConsumersMetadata() {
        int index = 1;
        for (int group = 0; group < consumerGroups; group++) {
            consumerStates.put("C" + index + "-PARES", "IDLE");
            consumerSums.put("C" + index + "-PARES", 0);
            index++;
            consumerStates.put("C" + index + "-IMPARES", "IDLE");
            consumerSums.put("C" + index + "-IMPARES", 0);
            index++;
            consumerStates.put("C" + index + "-PRIMOS", "IDLE");
            consumerSums.put("C" + index + "-PRIMOS", 0);
            index++;
        }
    }

    private void resetMetadata() {
        buffer.clear();
        producerState = "IDLE";
        criticalSectionOwner = "NINGUNA";
        consumerStates.replaceAll((k, v) -> "IDLE");
        consumerSums.replaceAll((k, v) -> 0);
        publishState();
    }

    private static List<Integer> readNumbers(Path path) throws IOException {
        List<Integer> values = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            values.add(Integer.parseInt(trimmed));
        }
        return values;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void joinThread(Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(1000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class SharedBuffer {
        private final LinkedList<NumberItem> queue = new LinkedList<>();
        private final ReentrantLock lock = new ReentrantLock(true);
        private final Condition changed = lock.newCondition();
        private final int capacity;

        private SharedBuffer(int capacity) {
            this.capacity = capacity;
        }

        boolean put(NumberItem item, java.util.function.Consumer<String> criticalOwner, Runnable onBlocked, AtomicBoolean running) {
            lock.lock();
            try {
                // Sección crítica: inserción en buffer finito.
                while (queue.size() >= capacity && running.get()) {
                    criticalOwner.accept("NINGUNA");
                    onBlocked.run();
                    awaitChanged();
                }
                if (!running.get()) {
                    return false;
                }
                criticalOwner.accept("PRODUCTOR");
                queue.add(item);
                changed.signalAll();
                return true;
            } finally {
                criticalOwner.accept("NINGUNA");
                lock.unlock();
            }
        }

        NumberItem takeOfType(
            NumberType type,
            java.util.function.Consumer<String> criticalOwner,
            Runnable onBlocked,
            AtomicBoolean producerDone,
            AtomicBoolean running
        ) {
            lock.lock();
            try {
                // Sección crítica: extracción filtrada por tipo de consumidor.
                while (running.get()) {
                    int idx = findIndex(type);
                    if (idx >= 0) {
                        criticalOwner.accept(Thread.currentThread().getName());
                        NumberItem item = queue.remove(idx);
                        changed.signalAll();
                        return item;
                    }
                    if (producerDone.get()) {
                        return null;
                    }
                    criticalOwner.accept("NINGUNA");
                    onBlocked.run();
                    awaitChanged();
                }
                return null;
            } finally {
                criticalOwner.accept("NINGUNA");
                lock.unlock();
            }
        }

        List<String> slotView() {
            lock.lock();
            try {
                List<String> slots = new ArrayList<>(capacity);
                for (NumberItem item : queue) {
                    slots.add(item.value() + ":" + item.type().label());
                }
                while (slots.size() < capacity) {
                    slots.add("_");
                }
                return slots;
            } finally {
                lock.unlock();
            }
        }

        int size() {
            lock.lock();
            try {
                return queue.size();
            } finally {
                lock.unlock();
            }
        }

        boolean isEmpty() {
            lock.lock();
            try {
                return queue.isEmpty();
            } finally {
                lock.unlock();
            }
        }

        void clear() {
            lock.lock();
            try {
                queue.clear();
                changed.signalAll();
            } finally {
                lock.unlock();
            }
        }

        void wakeAll() {
            lock.lock();
            try {
                changed.signalAll();
            } finally {
                lock.unlock();
            }
        }

        private int findIndex(NumberType type) {
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).type() == type) {
                    return i;
                }
            }
            return -1;
        }

        private void awaitChanged() {
            try {
                changed.awaitNanos(200_000_000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
