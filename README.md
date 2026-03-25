# SyncLab OS
**Visualizing concurrency, understanding synchronization.**

## 1) Project Overview
SyncLab OS is a Java 21 desktop application that unifies two classic Operating Systems synchronization problems in a single graphical environment:

1. Producer-Consumer
2. Dining Philosophers

The project was intentionally unified into one application so students can compare two synchronization models under one consistent UI and execution flow. This makes it easier to observe:

- shared resource contention
- critical-section control
- thread coordination
- blocking and wake-up behavior
- deadlock prevention strategies

From an academic perspective, the visual approach helps bridge theory (formal models and synchronization constraints) with runtime behavior that can be observed directly during demos.

## 2) Academic Purpose of the Project
This project is designed to support Operating Systems coursework by providing:

- a practical implementation of two canonical synchronization problems
- explicit use of Java concurrency primitives
- a visual interpretation of thread states and resource ownership
- evidence for technical documentation topics required in OS project statements:
  - formal problem modeling
  - synchronization strategy selection
  - critical-section handling
  - race-condition analysis
  - deadlock avoidance explanation

## 3) Main Features
- Single Java application with a single entry point.
- Main menu navigation to both synchronization modules.
- Real concurrency in both modules (threads + lock-based coordination).
- Integrated Producer-Consumer test scenarios loaded from project resources.
- Visual round-table animation for Dining Philosophers with named philosophers.
- Runtime controls for start/pause/resume/reset.
- State-focused UI designed for live academic demonstration.

## 4) Technology Stack
- **Language:** Java
- **Java target:** OpenJDK 21 (`maven.compiler.release=21`)
- **Build tool:** Maven
- **UI framework:** Swing
- **Concurrency primitives:** `Thread`, `ReentrantLock(true)`, `Condition`, `AtomicBoolean`

## 5) Requirements
- OpenJDK 21
- Maven 3.9+

Recommended:
- UTF-8 terminal/IDE to display accented labels correctly.

## 6) Build and Run Instructions
### Build
```bash
mvn clean compile
```

### Run
```bash
mvn exec:java
```

### Optional alternative (without Maven)
```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac --release 21 -d out (Get-ChildItem -Recurse -File src\main\java\*.java | ForEach-Object { $_.FullName })
java -cp "out;src/main/resources" com.synclabos.app.SyncLabOSApp
```

### Troubleshooting
- `mvn: command not found` -> install Maven and verify `mvn -v`.
- `Unsupported class file version` -> ensure Java 21 is active (`java -version`).
- Resource scenario not found -> run from project root so `src/main/resources` is available on classpath.

## 7) Project Structure
```text
src/main/java/com/synclabos/
  app/
    SyncLabOSApp.java
  ui/
    MainFrame.java
  common/
    EventSink.java
  producerconsumer/
    NumberItem.java
    NumberType.java
    ProducerConsumerState.java
    StateListener.java
    ProducerConsumerSimulation.java
    ProducerConsumerPanel.java
  diningphilosophers/
    PhilosopherState.java
    DiningState.java
    DiningTable.java
    DiningPhilosophersSimulation.java
    DiningPhilosophersPanel.java

src/main/resources/
  sample_numbers.txt
  walter/Pruebas/
    numeros.txt
    pares.txt
    doc.txt
```

### Key package responsibilities
- `app`: application bootstrap and entry point.
- `ui`: global navigation and composition of module panels.
- `producerconsumer`: problem model, synchronization logic, and module UI.
- `diningphilosophers`: problem model, synchronization logic, and module UI.
- `common`: shared callback contract for event logging.

## 8) Global Architecture
### Entry point
- `com.synclabos.app.SyncLabOSApp` creates `MainFrame` on the Swing EDT.

### Main window
- `MainFrame` uses `CardLayout` for screen navigation:
  - Home (menu)
  - Producer-Consumer panel
  - Dining Philosophers panel

### UI and simulation relationship
- Each module panel owns a simulation instance.
- Simulation threads run concurrency logic in background.
- UI refreshes state snapshots using a Swing `Timer`.
- Event text updates are marshalled through `SwingUtilities.invokeLater`.

### Responsiveness strategy
- Long-running logic stays off the EDT.
- EDT is used only for rendering and widget updates.

## 9) Application Flow
1. User launches `SyncLab OS`.
2. Main menu is shown with three actions:
   - Open Producer-Consumer
   - Open Dining Philosophers
   - Exit
3. User opens one module.
4. User controls simulation (start/pause/resume/reset).
5. User observes visual state transitions and summarized outputs.
6. User returns to main menu via `Back`.

## 10) Producer-Consumer Module
### Functional behavior
The module processes integer sequences and routes each value to one logical consumer type:

- primes
- evens
- odds

The UI presents:
- scenario selector
- scenario metadata
- simulation controls
- finite buffer visualization
- producer and critical-section indicators
- accumulated sums by category
- consumer state summary
- recent events panel

### Formal problem description
- **Producer:** 1 thread.
- **Consumers:** 3 per group (even, odd, prime).
- **Shared resource:** finite queue buffer.
- **Constraint:** producer waits when full; consumers wait when empty/no matching item.
- **Classification policy:** prime has priority over odd.

### Implemented requirement mapping
- Finite buffer: yes (`capacity` in `SharedBuffer`).
- Single producer: yes (`producerThread`).
- File/resource-based inputs: yes (`start(Path)` and integrated scenarios in `resources`).
- At least 3 consumers: yes (`consumerGroups=1` in current UI, yielding C1/C2/C3).
- Even/odd/prime consumer split: yes (`NumberType` routing).
- Prime-over-odd priority: yes (`NumberItem.fromValue`).
- Running sums: yes (`consumerSums` map and UI badges).
- Blocking/unblocking: explicit via `Condition.awaitNanos` and `signalAll`.

### User-visible states
- Producer: `IDLE`, `ACTIVO`, `INSERTANDO`, `PAUSADO`, `BLOQUEADO_BUFFER_LLENO`, `FINALIZADO`, `DETENIDO`
- Consumers: `IDLE`, `LISTO`, `ESPERANDO`, `EXTRAYENDO`, `BLOQUEADO_BUFFER_VACIO`, `BLOQUEADO_POR_TIPO`, `FINALIZADO`
- Buffer: slot view (`_` or `value:type`) and occupancy counter.

## 11) Dining Philosophers Module
### Functional behavior
The module simulates 5 philosophers around a circular table:

- Sócrates
- Platón
- Aristóteles
- Descartes
- Nietzsche

Visual model includes:
- circular table
- philosopher nodes with color-coded state
- fork ownership indicators
- compact status summaries
- optional debug dialog

### Formal problem description
- **Philosophers:** 5 concurrent threads.
- **Forks:** 5 shared resources.
- **States:** thinking, waiting, eating.
- **Eat condition:** philosopher can eat only when both left and right forks are available and request turn allows it.

### Synchronization behavior
- Requests are queued in FIFO order.
- Fork assignment is atomic under lock.
- Release triggers wake-up (`signalAll`).
- Lock fairness is enabled (`ReentrantLock(true)`).

## 12) Formal Problem Modeling
### Producer-Consumer formal model
- **Actors:** `P` producer, `C_even`, `C_odd`, `C_prime`.
- **Shared resource:** bounded queue `B`.
- **State variables:** `|B|`, producer state, consumer states, sums.
- **Blocking conditions:**
  - producer blocked if `|B| == capacity`
  - consumer blocked if no matching element in `B`
- **Safety constraints:**
  - `0 <= |B| <= capacity`
  - no unsynchronized queue access

### Dining Philosophers formal model
- **Actors:** `F0..F4` philosopher threads.
- **Shared resources:** forks `T0..T4`.
- **State variables:** philosopher state array, fork owner array, FIFO request queue.
- **Blocking condition:** philosopher waits until:
  - queue head is itself
  - both forks are free
- **Safety constraints:**
  - one fork owner at a time
  - atomic fork acquisition/release

## 13) Synchronization Strategy
### Primitives used
- `ReentrantLock(true)` for fair lock acquisition.
- `Condition` for blocking and signaling.
- `AtomicBoolean` for lifecycle flags (`running`, `paused`, `producerDone`).

### Why these primitives
- They provide explicit, inspectable control of critical sections.
- They map directly to OS synchronization concepts taught in class.
- Fair lock policy helps with starvation mitigation in queue-based access patterns.

## 14) Critical Sections
### Producer-Consumer
- Critical section: all reads/writes to shared queue.
- Guarded in:
  - `SharedBuffer.put(...)`
  - `SharedBuffer.takeOfType(...)`
  - `slotView()`, `size()`, `isEmpty()`, `clear()`
- Without protection:
  - overflow/underflow inconsistencies
  - duplicate or lost items
  - race on queue structure

### Dining Philosophers
- Critical section: updates to philosopher states, fork owners, and request queue.
- Guarded in:
  - `DiningTable.requestToEat(...)`
  - `DiningTable.releaseForks(...)`
  - `DiningTable.snapshot()`
- Without protection:
  - simultaneous fork ownership
  - inconsistent philosopher/fork states

## 15) Race Conditions Analysis
### Producer-Consumer
Possible races:
- concurrent producer/consumer queue mutation
- state map visibility inconsistencies

Mitigations:
- queue operations under lock
- state snapshots copied before UI propagation (`new HashMap<>(...)`)

### Dining Philosophers
Possible races:
- parallel fork acquisition
- state transitions while rendering

Mitigations:
- all table state changes guarded by one fair lock
- UI consumes immutable snapshot record (`DiningState`)

## 16) Deadlock Prevention
### Producer-Consumer
- Blocking is intentional (full/empty/no-matching-type waits).
- Deadlock is avoided by condition signaling (`signalAll`) and loop re-check patterns.

### Dining Philosophers
Classic deadlock is prevented by:
- FIFO request queue
- atomic acquisition of both forks only when both are free
- no partial hold-and-wait of one fork per philosopher

Starvation is mitigated by FIFO turn discipline plus fair locking.

## 17) Animation and UI Explanation
### Design intent
- Visual understanding should be primary.
- Text logs are supporting evidence (not the only explanation channel).

### Producer-Consumer interpretation
- Slot grid = finite shared buffer.
- Green slot = occupied element.
- `_` slot = free space.
- Producer and critical labels indicate active thread/section.
- Sum badges show cumulative processing per category.

### Dining Philosophers interpretation
- Colored philosopher nodes represent state:
  - blue-ish: thinking
  - yellow-ish: waiting
  - green-ish: eating
- Fork bars:
  - gray: free
  - red: occupied
- Circular placement reinforces formal round-table model.
- Debug dialog is secondary and optional.

## 18) Integrated Test Scenarios / Validation
Integrated scenarios are loaded from project resources:

- `walter/Pruebas/numeros.txt`
- `walter/Pruebas/pares.txt`
- `walter/Pruebas/doc.txt`

UI labels for these scenarios are neutral:
- `Prueba 1 - Secuencia Mixta`
- `Prueba 2 - Dominio de Pares`
- `Prueba 3 - Validación Rápida`

What each demonstrates:
- mixed sequence behavior
- parity-heavy behavior
- quick validation behavior

Validation evidence during execution:
- buffer never exceeds capacity
- blocking states appear as expected
- sums increase consistently with consumed values
- philosopher/fork visual state transitions remain coherent

## 19) User Manual
### Prerequisites
- OpenJDK 21
- Maven 3.9+

### Launch
```bash
mvn clean compile
mvn exec:java
```

### Main menu usage
- Click `Productor-Consumidor` or `Filósofos Comensales`.
- Click `Salir` to close application.

### Producer-Consumer usage
1. Select a built-in scenario from the list.
2. Click `Cambiar prueba` if needed.
3. Click `Ejecutar prueba`.
4. Use `Pausar`, `Reanudar`, `Reiniciar` as needed.
5. Optionally use `Archivo externo` to load a custom integer dataset.

### Dining Philosophers usage
1. Click `Iniciar`.
2. Observe table and state colors.
3. Use `Pausar`, `Reanudar`, `Reiniciar`.
4. Optional: open `Ver debug` for textual event details.

### Return navigation
- Use `Volver al menú` in each module.

### Common issues
- Wrong Java version -> install/set Java 21.
- Maven not installed -> use alternative `javac` path.
- Invalid custom file -> ensure one integer per line.

## 20) Suggested Diagrams
Recommended diagrams to add in external documentation:

1. **Global Architecture Diagram**
   - App entry, main frame, module panels, simulation engines, shared state snapshots.
2. **Main Menu Flowchart**
   - Start -> menu -> module selection -> back/exit.
3. **Producer-Consumer Flowchart**
   - Scenario load -> producer classify -> put/take -> sum update -> finish.
4. **Dining Philosophers Flowchart**
   - Think -> request -> wait -> eat -> release -> repeat.
5. **Thread-Resource Interaction Diagram**
   - Producer/consumers vs shared buffer and lock.
6. **Critical Section Diagram**
   - Locked regions and state transitions in both modules.
7. **Deadlock Avoidance Diagram (Dining)**
   - FIFO queue + dual-fork availability gate.

## 21) Code Snippets
### Entry point
```java
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        MainFrame frame = new MainFrame();
        frame.setVisible(true);
    });
}
```

### Producer/consumer classification logic
```java
public static NumberItem fromValue(int value) {
    if (isPrime(value)) return new NumberItem(value, NumberType.PRIME);
    if (value % 2 == 0) return new NumberItem(value, NumberType.EVEN);
    return new NumberItem(value, NumberType.ODD);
}
```

### Shared buffer synchronization (put)
```java
lock.lock();
try {
    while (queue.size() >= capacity && running.get()) {
        onBlocked.run();
        changed.awaitNanos(200_000_000L);
    }
    queue.add(item);
    changed.signalAll();
} finally {
    lock.unlock();
}
```

### Shared buffer synchronization (take by type)
```java
lock.lock();
try {
    while (running.get()) {
        int idx = findIndex(type);
        if (idx >= 0) {
            NumberItem item = queue.remove(idx);
            changed.signalAll();
            return item;
        }
        if (producerDone.get()) return null;
        onBlocked.run();
        changed.awaitNanos(200_000_000L);
    }
    return null;
} finally {
    lock.unlock();
}
```

### Dining philosophers resource acquisition
```java
if (requestQueue.peek() != null
    && requestQueue.peek() == philosopherId
    && forksAvailable(philosopherId)) {
    requestQueue.poll();
    takeForks(philosopherId);
    states[philosopherId] = PhilosopherState.EATING;
    meals[philosopherId]++;
    return;
}
```

### UI state refresh from snapshots
```java
Timer timer = new Timer(120, e -> refreshState());
timer.start();
```

## 22) Limitations
- No automated JUnit test suite is currently included.
- Some UI strings with accents may depend on local font/encoding rendering.
- Producer-Consumer UI currently runs one consumer group by default (3 consumers), though simulation supports grouped scaling.
- Performance metrics/telemetry are not yet built in.

## 23) Future Improvements
- Add automated unit/integration tests for synchronization invariants.
- Add runtime configuration for philosopher count and consumer groups.
- Add exportable execution traces for report generation.
- Add optional metrics panel (throughput, wait times, contention).
- Provide localization/i18n support.

## 24) Screenshots (Placeholders)
> Replace these placeholders with real captures from your environment.

### Main Menu
`docs/screenshots/main-menu.png`

### Producer-Consumer (Scenario Selection)
`docs/screenshots/pc-scenario-selection.png`

### Producer-Consumer (Running)
`docs/screenshots/pc-running-buffer.png`

### Dining Philosophers (Round Table Running)
`docs/screenshots/dp-round-table.png`

### Dining Philosophers (Debug Dialog)
`docs/screenshots/dp-debug-dialog.png`

## 25) License
**[License placeholder]**

Add your chosen license here (for example, MIT, Apache-2.0, GPL-3.0, or institutional policy license).
