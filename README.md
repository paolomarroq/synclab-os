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
