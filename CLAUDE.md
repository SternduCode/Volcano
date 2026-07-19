# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all modules
./gradlew build

# Build a single module
./gradlew :DataTransfer:build
./gradlew :Multicore:build
./gradlew :Encryption:build
./gradlew :Bridge:build
./gradlew :Balancer:build

# Publish Multicore to local Maven (it has maven-publish configured)
./gradlew :Multicore:publishToMavenLocal

# Generate sources jar (Multicore only)
./gradlew :Multicore:sourcesJar
```

There is no test framework wired in. `EncryptionTest.kt` and `BridgeTester.kt` are scratch/experimentation files (not tests against the codebase) — `EncryptionTest.kt` for trying things in the Encryption module, `BridgeTester.kt` for the Bridge module.

## Module Architecture

The project is a Gradle multi-module library with Java Platform Module System (JPMS) enabled. All source files live in `src/` (not `src/main/kotlin/`). The dependency graph is:

```
Balancer        (no deps on other modules)
Multicore       (no deps on other modules)
Encryption  →   Multicore
DataTransfer →  Multicore, Encryption
Bridge       →  Multicore, Balancer, DataTransfer, Encryption
```

All modules use JVM 25 and Kotlin 2.x. Each module contains a `Fix.java` file; this exists so the `compileJava` task can use `--patch-module` to see compiled Kotlin classes, which is required for mixed Java/Kotlin JPMS modules.

### Multicore

`Multicore` (singleton object) is the central scheduler. It wraps a `ScheduledThreadPoolExecutor` backed by virtual threads. Tasks are registered with keys scoped to the calling class (uses `StackWalker` to identify the caller, so task keys are per-class).

- `scheduleTask(delay, block)` — one-shot
- `scheduleTaskAtFixedRate(key, delay, millis, ...)` — repeating
- `scheduleTaskWithFixedDelay(key, delay, millis, ...)` — fixed delay variant
- `removeTask(key)` — removes by (callerClass, key) pair

`RepeatingTaskHandler` is a **deprecated** legacy singleton that forwards calls to `Multicore`. `MultiCore` (capital C) is also deprecated in favor of `Multicore`.

### Encryption

`Crypter` is the abstract base for all ciphers. `CrypterProvider` discovers available ciphers at JVM startup (checks `Security.getAlgorithms("Cipher")`) and maps them to short codes:
- `1` → AES/GCM/NoPadding (128-bit key)
- `10` → AES_256/GCM/NoPadding
- `20` → ChaCha20-Poly1305

`KeyExchange` is the abstract key agreement interface. `DiffieHellmanWithKyber` (post-quantum hybrid) is the default. During negotiation the two sides agree on the highest shared cipher code.

### DataTransfer

Wire format: `[type: Byte][length: Int (big-endian)][data: ByteArray]`. Max packet size is 1 GiB.

`DataTransferClient` (abstract) owns the handshake state machine, packet dispatch, and ping/pong. `DataTransferSocket` extends it over a plain TCP `java.net.Socket`. `DataTransferServerSocket` wraps `ServerSocket` and returns `DataTransferSocket` from `accept()`.

**Reserved type bytes** (not encrypted, not user-definable):
- `-1` close signal
- `-2` / `-3` / `-4` / `-5` handshake phases
- `-126` / `-127` raw and encrypted ping/pong

When `secureMode=true` the host initiates a 3-phase handshake automatically on connection. Encryption/decryption is transparent after `initialized` becomes `true`. If a `Crypter` reaches its encryption limit it triggers a re-handshake.

`Connector` wraps a `DataTransferClient` to multiplex a logical sub-channel over a single type byte, with its own send/receive API.

### Bridge

`BridgeServer` is an encrypted broker. Clients connect to it and either:
- **Host** (type byte `1`): receive a random base64 code, clients can join using that code
- **Join** (type byte `3`): connect to a host by code, get a multiplexed channel (type `5`)
- **Connect** (type byte `2`): proxy a direct TCP connection through the server

`BridgeClient` is the client-side counterpart. `HostConnector` wraps the two `Connector`s (for control and data) returned to a hosting client. Default port is in `BridgeUtil.DEFAULT_PORT`.

## Key Patterns

**Logging**: always use `LoggingUtil.getLogger("Name")` (not `Logger.getLogger` directly). Optionally enable file logging with `LoggingUtil.setLogToFile(true)` before any logger is created; logs go to `logs/`.

**Debug mode**: pass `-Ddebug=true` to dump raw packets as `.pckt` files in the working directory.

**Handles**: `DataTransferClient.setHandle(type, handler)` registers a callback for a packet type. Handle ownership is enforced by the calling class — only the class that set a handle can replace or remove it.
