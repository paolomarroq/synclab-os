package com.synclabos.diningphilosophers;

import com.synclabos.common.EventSink;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DiningPhilosophersSimulation {
    private final int philosopherCount;
    private final DiningTable table;
    private final EventSink eventSink;
    private final Consumer<DiningState> stateConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final List<Thread> threads = new CopyOnWriteArrayList<>();

    public DiningPhilosophersSimulation(
        int philosopherCount,
        EventSink eventSink,
        Consumer<DiningState> stateConsumer
    ) {
        this.philosopherCount = philosopherCount;
        this.eventSink = eventSink;
        this.stateConsumer = stateConsumer;
        this.table = new DiningTable(philosopherCount, eventSink);
        publishState();
    }

    public synchronized void start() {
        stop();
        running.set(true);
        paused.set(false);
        eventSink.log("Simulación de filósofos iniciada.");
        for (int i = 0; i < philosopherCount; i++) {
            int philosopherId = i;
            Thread thread = new Thread(() -> runPhilosopher(philosopherId), "Filosofo-" + i);
            threads.add(thread);
            thread.start();
        }
    }

    public synchronized void pause() {
        paused.set(true);
        eventSink.log("Simulación pausada.");
    }

    public synchronized void resume() {
        paused.set(false);
        table.wakeAll();
        eventSink.log("Simulación reanudada.");
    }

    public synchronized void stop() {
        running.set(false);
        paused.set(false);
        table.wakeAll();
        for (Thread thread : new ArrayList<>(threads)) {
            joinThread(thread);
        }
        threads.clear();
        publishState();
    }

    private void runPhilosopher(int philosopherId) {
        Random random = new Random(100 + philosopherId);
        while (running.get()) {
            waitIfPaused();
            eventSink.log("Filósofo " + philosopherId + " pensando.");
            sleep(500 + random.nextInt(600));
            if (!running.get()) {
                break;
            }
            table.requestToEat(philosopherId, running, paused);
            publishState();
            sleep(450 + random.nextInt(500));
            table.releaseForks(philosopherId);
            publishState();
            sleep(250 + random.nextInt(350));
        }
    }

    private void waitIfPaused() {
        while (paused.get() && running.get()) {
            sleep(120);
        }
    }

    private void publishState() {
        stateConsumer.accept(table.snapshot());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void joinThread(Thread thread) {
        try {
            thread.join(1000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
