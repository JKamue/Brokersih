# Brokerish — CLAUDE.md

Custom MQTT 5.0 broker written in Kotlin, built **as a learning project** by the owner.

## Claude's role in this repo (important)

The owner writes and understands all code themselves. AI is used in an **advisory role only**:
review, explain, discuss design trade-offs, point at spec sections, suggest approaches.
**Do not implement whole features unprompted.** Small snippets to illustrate a point are fine;
"here is the finished feature" is not. When finding bugs, report and explain them rather than
silently fixing them, unless explicitly asked to fix.

## Project goals

- Get QoS 0 feature-complete first, then QoS 1, maybe QoS 2 later.
- Clean OOP architecture with clear separation of concerns **and** high performance
  (many concurrent clients, data handled with as few copies as possible — ideally one).
- Not intended for production use.

## Modules (Gradle multi-project)

| Module | Package root | Depends on | Responsibility |
|---|---|---|---|
| `packets` | `de.jkamue.mqtt` | — | Pure domain model: packet data classes, value objects (`@JvmInline`), reason codes, exceptions. No I/O, no parsing. |
| `parser` | `de.jkamue.mqtt.parser`, `de.jkamue.mqtt.encoder` | packets | Byte-level parsing (`PacketParser` + per-packet parsers) and encoding (`PacketEncoder` + per-packet encoders, scatter/gather style). |
| `logic` | `de.jkamue.mqtt.logic` | packets, coroutines | Broker brain: `MqttServer` actor, `ClientManager`, `SubscriptionTree`. No sockets, no buffers — only `PayloadManager`/`OutgoingMessage` abstractions. Only module with tests. |
| `server` | `de.jkamue.mqtt.server` | packets, parser, logic, ktor | Network layer: TCP accept loop, per-connection reader/writer coroutines, `BufferPool`, keep-alive timeout. Logging via kotlin-logging → logback. |

## Architecture in one paragraph

`server/Application.kt` accepts sockets; each connection gets a reader coroutine (reads fixed
header + remaining length, leases an 8 KB buffer from `BufferPool`, parses via `PacketParser`)
and a writer coroutine (drains a per-client `Channel<OutgoingMessage>`, encodes with
`PacketEncoder.encodeScatter` into an array of ByteBuffers written sequentially — no
concatenation copy). All broker state is owned by a **single actor**: `MqttServer` consumes a
`Channel<ServerCommand>` (`ClientConnected`, `ClientDisconnected`, `DisconnectClient`,
`PacketReceived`), so logic-side state mutation is effectively single-threaded. Payload buffer
lifetime crosses the module boundary via `PayloadManager` (single release action, or
`ReferenceCountedRelease` for fan-out to N subscribers) so `logic` never knows about the pool.

## Performance conventions (deliberate, keep them)

- **Topics are never Strings** on the hot path. `Topic`/`TopicFilter` wrap read-only
  `ByteBuffer`s; segment access via lazy `firstSegment`/`remainingSegments` ByteBuffer slices.
  `SubscriptionTree` nodes key children by `ByteBuffer`.
- Publish payloads are zero-copy: slices of the leased read buffer, released via reference
  counting after the last subscriber write (`fix 94935f7` — duplicate buffers when multiple
  consumers read them).
- Data that outlives the packet (subscriptions' topic filters, will, auth/correlation data)
  **must** be copied out of the leased buffer via `createCopy()` — otherwise the whole 8 KB
  buffer is pinned (comments in parsers mark these spots).
- Encoders produce scatter arrays; small constant packets (PINGRESP) use a shared read-only
  static buffer.
- `setOnce` inline-extension pattern in property parsers: detects duplicate properties per
  spec, lambda-deferred error-message interpolation, inline to avoid value-class boxing.

## Code conventions

- Value objects are `@JvmInline value class` (ClientId, Interval, ReceiveMaximum, …);
  validation lives in their `init` blocks, throwing the matching `MqttException` subclass.
- Comments cite MQTT 5.0 spec sections (`// 3.1.2.11.3 - ...`) and normative statement IDs
  (`MQTT-3.1.4-3`). Keep doing this — it is how the code documents "why".
- Tests can be tagged `@MandatoryNormativeStatementTest("MQTT-x.y.z-n")` to link them to spec
  normative statements.
- Commit style: conventional commits with optional module scope — `feat:`, `fix:`, `perf:`,
  `refactor(logic):`, `test(logic):`, `docs:`.
- Test style (logic module): `runTest` + `TestServer`/`TestClient` helpers + `drain()`
  (advanceUntilIdle); mockk for `ClientManager`/`SubscriptionTree` verification;
  given/when/then comments; backticked sentence test names.

## Build & run

- Tests: `.\gradlew.bat test` (only `logic` has tests today).
- Real entry point: `de.jkamue.ApplicationKt.main()` — binds hardcoded `127.0.0.1:9002`,
  reads `server/src/main/resources/application.yaml` (`brokerish:` section →
  `BrokerishConfig`).
- The ktor scaffolding is stale: `server/build.gradle.kts` sets
  `mainClass = io.ktor.server.netty.EngineMain` and application.yaml references a
  `de.jkamue.ApplicationKt.module` function that no longer exists, so the Gradle `run` task
  does not launch the broker. Run `main()` from the IDE instead. README's ktor section is
  also generator boilerplate.

## Current feature state (as of 2026-07)

Implemented: CONNECT/CONNACK (incl. will + properties parsing), PINGREQ/PINGRESP,
SUBSCRIBE/SUBACK, PUBLISH QoS 0 receive + fan-out, `+` wildcard, subscription identifiers,
keep-alive timeout disconnect, session takeover (MQTT-3.1.4-3), max-QoS config.
Not implemented: `#` wildcard, retain, QoS 1/2 (packet identifiers, PUBACK…), UNSUBSCRIBE,
AUTH, session state/expiry, will publishing on disconnect (TODO in MqttServer), topic
validation (MQTT-4.7), topic aliases. PUBLISH fixed-header flags (dup/qos/retain) are
currently discarded — `readMqttPacket` only passes the `ControlPacketType`, not the flag
bits, to the parser (known TODO, blocks QoS 1).

## Known gotchas / open findings (verify before relying on)

Reported to owner 2026-07-18, unfixed at time of writing:

1. `SubscribePacketParser.parseSubscriptionOptions` reads the options byte with the bit
   layout mirrored: QoS from bits 6–7 (spec §3.8.3.1: bits 0–1), No Local bit 5 (spec: 2),
   RAP bit 4 (spec: 3), Retain Handling bits 2–3 (spec: 4–5). Masked today because broker is
   QoS-0-only; breaks the moment QoS 1 lands.
2. `PublishPropertiesParser`: SUBSCRIPTION_IDENTIFIER is read with `getTwoByteInt()` but is a
   Variable Byte Integer per spec; its error label says "responseTopic"; the handlers map
   also has RESPONSE_TOPIC listed twice.
3. `Application.kt` first-packet check: if `readMqttPacket` returns null (peer closes before
   sending anything) the `packetAndBuffer!!` NPEs.
4. Reader loop conflates EOF with keep-alive timeout (`readMqttPacket` returning null makes
   `withTimeoutOrNull` return null). Consequence: on session takeover the *old* connection's
   teardown sends a disconnect command keyed only by `ClientId`, which removes the **new**
   client's registration and subscriptions. Fix idea discussed: identify connections by
   generation/channel identity, not ClientId alone.
5. `PublishEncoder.kt` has a leftover debug line `MQTTByteBuffer.wrap(topicName).getString()`
   allocating a String per outgoing publish (defeats the no-string-topics rule).
6. Re-subscribe with same client+filter but different options adds a second tree entry
   (duplicate delivery) instead of replacing per MQTT-3.8.4-3.
7. `getFourByteInt` is signed → four-byte values ≥ 2³¹ (e.g. session expiry 0xFFFFFFFF =
   never) come out negative.
8. `AsyncLogger` formats `System.nanoTime()` as if it were wall-clock time — timestamps have
   an arbitrary origin.
