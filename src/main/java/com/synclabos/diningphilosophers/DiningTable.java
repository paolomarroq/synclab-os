package com.synclabos.diningphilosophers;

import com.synclabos.common.EventSink;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DiningTable {
    private final int count;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition changed = lock.newCondition();
    private final Queue<Integer> requestQueue = new ArrayDeque<>();
    private final PhilosopherState[] states;
    private final Integer[] forkOwner;
    private final int[] meals;
    private final EventSink eventSink;

    public DiningTable(int count, EventSink eventSink) {
        this.count = count;
        this.eventSink = eventSink;
        this.states = new PhilosopherState[count];
        this.forkOwner = new Integer[count];
        this.meals = new int[count];
        for (int i = 0; i < count; i++) {
            states[i] = PhilosopherState.THINKING;
            forkOwner[i] = null;
            meals[i] = 0;
        }
    }

    public void requestToEat(int philosopherId, AtomicBoolean running, AtomicBoolean paused) {
        lock.lock();
        try {
            states[philosopherId] = PhilosopherState.WAITING;
            if (!requestQueue.contains(philosopherId)) {
                requestQueue.offer(philosopherId);
            }
            // Cola FIFO + lock justo: evita interbloqueo y reduce riesgo de inanición.
            eventSink.log("Filósofo " + philosopherId + " intenta adquirir tenedores.");
            while (running.get()) {
                awaitIfPaused(paused);
                if (!running.get()) {
                    break;
                }
                if (requestQueue.peek() != null && requestQueue.peek() == philosopherId && forksAvailable(philosopherId)) {
                    requestQueue.poll();
                    takeForks(philosopherId);
                    states[philosopherId] = PhilosopherState.EATING;
                    meals[philosopherId]++;
                    eventSink.log("Filósofo " + philosopherId + " entra a sección crítica (comiendo).");
                    return;
                }
                eventSink.log("Filósofo " + philosopherId + " bloqueado: esperando tenedores.");
                awaitChanged();
            }
        } finally {
            lock.unlock();
        }
    }

    public void releaseForks(int philosopherId) {
        lock.lock();
        try {
            putForks(philosopherId);
            states[philosopherId] = PhilosopherState.THINKING;
            eventSink.log("Filósofo " + philosopherId + " liberó tenedores y vuelve a pensar.");
            changed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public DiningState snapshot() {
        lock.lock();
        try {
            List<PhilosopherState> statesCopy = new ArrayList<>(count);
            List<String> forkCopy = new ArrayList<>(count);
            List<Integer> mealsCopy = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                statesCopy.add(states[i]);
                forkCopy.add(forkOwner[i] == null ? "Libre" : "F" + forkOwner[i]);
                mealsCopy.add(meals[i]);
            }
            return new DiningState(statesCopy, forkCopy, mealsCopy);
        } finally {
            lock.unlock();
        }
    }

    public void wakeAll() {
        lock.lock();
        try {
            changed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean forksAvailable(int philosopherId) {
        int left = leftFork(philosopherId);
        int right = rightFork(philosopherId);
        return forkOwner[left] == null && forkOwner[right] == null;
    }

    private void takeForks(int philosopherId) {
        forkOwner[leftFork(philosopherId)] = philosopherId;
        forkOwner[rightFork(philosopherId)] = philosopherId;
    }

    private void putForks(int philosopherId) {
        if (forkOwner[leftFork(philosopherId)] != null && forkOwner[leftFork(philosopherId)] == philosopherId) {
            forkOwner[leftFork(philosopherId)] = null;
        }
        if (forkOwner[rightFork(philosopherId)] != null && forkOwner[rightFork(philosopherId)] == philosopherId) {
            forkOwner[rightFork(philosopherId)] = null;
        }
    }

    private int leftFork(int philosopherId) {
        return (philosopherId - 1 + count) % count;
    }

    private int rightFork(int philosopherId) {
        return philosopherId;
    }

    private void awaitChanged() {
        try {
            changed.awaitNanos(180_000_000L);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void awaitIfPaused(AtomicBoolean paused) {
        while (paused.get()) {
            awaitChanged();
        }
    }
}
