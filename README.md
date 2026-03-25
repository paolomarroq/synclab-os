<<<<<<< HEAD
# SyncLab OS (Java, OpenJDK 21)

Proyecto unificado del Proyecto No. 1 de Sistemas Operativos con:
- Productor-Consumidor (concurrencia real con `Thread`, `ReentrantLock` y `Condition`).
- Filósofos Comensales (concurrencia real con `Thread` y monitor central con cola justa FIFO).

## Requisitos
- OpenJDK 21
- Maven 3.9+

## Compilar
```bash
mvn clean compile
```

## Ejecutar
```bash
mvn exec:java
```

## Ejecutar jar compilado
```bash
mvn -DskipTests package
java -cp target/classes com.synclabos.app.SyncLabOSApp
```
=======
# synclab-os
Desktop Java 21 application that visualizes the classic synchronization problems Producer-Consumer and Dining Philosophers through concurrent simulations and a graphical user interface.
>>>>>>> 0991ff37f099faff72b222bb145ea78d0fa223f6
