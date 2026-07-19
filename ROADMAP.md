# Brokerish Roadmap

Ordering principle: real-world, high-impact behavior over spec trivia. Tests come first
because everything after them (refactors, compliance work, any native experiment) needs a
safety net to be fun instead of scary.

## Phase 0 — Known bug fixes (small, do alongside Phase 1)

Fix as the corresponding tests get written — each of these is a test case first:

- [ ] Subscription options byte parsed with mirrored bit layout (§3.8.3.1) — blocks QoS 1
- [ ] `NotImplementedError` escapes the connection handler's `catch (e: Exception)` (it is an
      `Error`, not an `Exception`): any unimplemented packet type — today a client's normal
      DISCONNECT — kills the coroutine without sending `ClientDisconnected`, leaking the
      client's registration and subscriptions in the actor forever. Fix independently of
      implementing DISCONNECT: have the parser's `else` branch throw a `MqttException`
      (or catch `Throwable` boundaries deliberately) so unknown packets disconnect cleanly.
- [ ] EOF conflated with keep-alive timeout → session takeover tears down the *new* session
      (identify connections by generation/channel, not ClientId alone) — write the test here,
      the structural fix is Phase 3.2's Session/Connection split
- [ ] NPE when a client connects and closes without sending anything (`packetAndBuffer!!`)
- [ ] PUBLISH subscription identifier read as two-byte int instead of Variable Byte Integer;
      duplicate RESPONSE_TOPIC handler entry; copy-pasted error label
- [ ] Dead debug line in `PublishEncoder` (`.getString()` on the topic per publish)
- [ ] Keep alive = 0 means "never disconnect" (§3.1.2.10); today it computes a 0s timeout
      and kicks the client instantly
- [ ] Re-subscribe with same filter should *replace* the subscription (MQTT-3.8.4-3), not add
      — falls out of Phase 3.2's session-owned subscriptions
- [ ] `getFourByteInt` signed overflow for values ≥ 2³¹ (session expiry 0xFFFFFFFF)
- [ ] (Optional, decided) actor owns all state → replace ConcurrentHashMap/CopyOnWriteArraySet
      with plain collections + a comment declaring the ownership model — lands as part of
      Phase 3.3's actor-discipline work

Found in the 2026-07-19 parser/encoder audit:

- [ ] `DisconnectEncoder` encodes remaining length 3 but only 2 bytes follow (reason code +
      property length) — every server-sent DISCONNECT is malformed on the wire; strict
      clients stall waiting for the missing byte
- [ ] Will response topic stored as an *uncopied* slice of the leased read buffer
      (`WillPropertyParser` RESPONSE_TOPIC — unlike CORRELATION_DATA right below it, which
      does `createCopy()`) — dangling data once the buffer is reused; latent until will
      publishing lands, then silent corruption
- [ ] `MqttException` takes `reasonCode` as a constructor param but never stores it (`val`
      missing) — catch sites can't recover the code; the Phase 3.5 exception → reason-code
      mapper depends on it
- [ ] `RequestResponseInformation`/`RequestProblemInformation` parse `byte == 1`, silently
      mapping invalid values (e.g. 2) to false — spec says values other than 0/1 are a
      protocol error (§3.1.2.11.6/.7)
- [ ] `MqttEncoderHelpers` has two identical VBI-size functions (`variableByteIntegerLength`
      and `variableByteIntSize`) — merge before they drift

## Phase 1 — Test foundation

The parser is the most testable code in the repo (pure functions, bytes in → objects out) and
currently has zero tests. Highest value per effort in the whole roadmap.

- [ ] **Golden-packet parser tests**: hand-crafted byte arrays (or captured from mosquitto/
      MQTTX with Wireshark) for CONNECT (with/without will, properties, auth), SUBSCRIBE,
      PUBLISH, PINGREQ; assert parsed objects field by field. Include malformed-packet cases
      (bad remaining length, unknown properties, duplicate properties, reserved bits set).
- [ ] **Encoder tests**: encode CONNACK/SUBACK/PUBLISH/DISCONNECT, assert exact bytes.
- [ ] **Round-trip tests** where both directions exist (PUBLISH in → PUBLISH out).
- [ ] **Logic tests expansion**: subscribe→publish→receive flow, wildcard matching table
      (`a/+/c` vs topics), fan-out release counting (payload released exactly once after N
      subscribers), subscription replace semantics.
- [ ] **One end-to-end smoke test**: bind a real socket, connect with a real client library
      (HiveMQ MQTT client or Eclipse Paho), subscribe, publish, receive, disconnect. This is
      the only test that proves the wire format against an independent implementation.
- [ ] Consider a `SubscriptionTree` property/fuzz test later (random topic/filter pairs vs. a
      naive reference matcher) — cheap confidence for the trickiest data structure.

## Phase 2 — CI + Docker

Cheap, high leverage, and independent of feature work — do early.

- [ ] Clean up stale ktor scaffolding first: `mainClass` points at `EngineMain` and
      application.yaml references a nonexistent `module` — a fat jar built today would not
      start the broker. Point `mainClass` at `de.jkamue.ApplicationKt`, remove the dead
      routing config, make bind host/port come from application.yaml.
- [ ] GitHub Actions: `./gradlew build test` on push/PR. Add the e2e smoke test to the
      pipeline once it exists.
- [ ] Multi-stage Dockerfile: gradle build stage → slim JRE stage (temurin-jre or distroless
      java). Expose configured port, run as non-root.
- [ ] Publish image as a pipeline artifact (GitHub Container Registry is free for public
      repos).
- [ ] Stretch: interop job in CI — run the container plus `mosquitto_pub`/`mosquitto_sub` or
      MQTTX CLI against it as a black-box conformance smoke.

## Phase 3 — Architectural groundwork (review changes 1–5)

The load-bearing changes from the 2026-07 architecture review. Deliberately *before* the
feature fill: every Phase 4 feature (wills, errors, capabilities) lands on this structure
once instead of being built on the old one and rewritten. Order inside the phase matters —
smallest and most assumption-stopping first.

1. [ ] **Header flags plumb-through** (review #5): `readMqttPacket` passes the fixed-header
       flag bits, `parsePacket(bytes, type, flags)`, `PublishPacketParser` reads real
       dup/qos/retain, encoders take flags as parameters instead of hardcoding `0b00110000`.
       Every week this waits, another parser/encoder bakes in the QoS-0 assumption.
       Unblocks retain (Phase 4.3) and QoS 1 (Phase 6).
2. [ ] **Session/Connection split** (review #1): `Connection` = socket-scoped (outgoing
       channel, keep-alive, negotiated limits, generation identity); `Session` =
       clientId-scoped (subscriptions, will, expiry — later all QoS 1 state). Every
       `ServerCommand` carries connection identity; the actor ignores commands from stale
       connections → fixes the Phase 0 takeover/EOF bug structurally. Sessions own their
       subscription lists; `SubscriptionTree` becomes a pure routing index → resubscribe-
       replace (MQTT-3.8.4-3) and the `removeSubscriptionsFor` side-map leak go with it.
3. [ ] **Actor discipline** (review #2): handlers never suspend — fan-out via `trySend` with
       a per-connection QoS 0 drop policy (count drops, disconnect past a threshold;
       spec-legal for at-most-once). Bound `commandChannel` and `BufferPool` with an explicit
       exhaustion policy. Plain collections + ownership comment (Phase 0's optional item).
       Done when: a load test with one deliberately stalled subscriber leaves every other
       client unaffected.
4. [ ] **Leak-proof payload lifetime** (review #4): `onUndeliveredElement` on outgoing
       channels so a closed connection releases its queued payloads; `afterSend` guaranteed
       via try/finally around the socket write; debug-mode leak detector in `BufferPool`
       (lease age / outstanding count). Done when: every test run ends with zero
       outstanding leases.
5. [ ] **CONNECT handshake owner** (review #3): one `ConnectionOpened(connectPacket,
       connection)` command; the actor handles takeover + `sessionPresent` + will storage +
       CONNACK atomically — `Application.kt` stops making protocol decisions. Includes the
       exception → reason-code mapping component that Phase 4.5 (graceful errors) and
       Phase 4.7 (capability advertisement) then build on directly.

**Decision recorded for Phase 6** (review #4, discussed 2026-07-19): zero-copy stays the
QoS 0 fast path. Messages entering QoS ≥ 1 storage get **one copy at enqueue** — payload
*and* topic, both are slices of the leased read buffer — into right-sized pooled buffers,
behind a migratable `PayloadHandle`. Pin-while-in-flight / copy-on-eviction is a later
profiler-gated optimization the handle seam keeps possible; build it only if the Phase 5
rig shows the copy mattering.

## Phase 4 — QoS 0 compliance, impact-ordered

What a real client/dashboard/IoT fleet actually hits, most-used first:

1. [ ] **UNSUBSCRIBE / UNSUBACK** — every nontrivial client uses it; currently unparsed.
2. [ ] **`#` multi-level wildcard** — ubiquitous (`sensors/#`). Plus the §4.7 rules that come
       with wildcards: `#` only as last segment, wildcards must not match `$`-prefixed topics,
       reject wildcards in PUBLISH topic names.
3. [ ] **Retained messages** — flagship real-world feature (state topics, dashboards resume
       instantly). Prerequisite: header flags plumb-through, done in Phase 3.1.
4. [ ] **Will message publishing (LWT)** — the other flagship IoT feature; TODO already in
       `MqttServer`. Needs the client DISCONNECT packet parsed too, so a *normal* disconnect
       suppresses the will (§3.14) while an abnormal one fires it.
5. [ ] **Graceful protocol errors** — on malformed packets send DISCONNECT (or error CONNACK
       during connect) with the proper reason code before closing, instead of silently
       dropping the socket. Huge interop/debugging quality-of-life; the reason-code enums and
       exception hierarchy already exist and map 1:1. This is the wiring of Phase 3.5's
       exception → reason-code mapper to every error site.
6. [ ] **PUBLISH property forwarding** — outgoing publishes currently encode *only* the
       subscription identifier; payload format indicator, response topic, correlation data,
       content type and user properties MUST be forwarded unaltered
       (MQTT-3.3.2-4/-15/-16/-17/-18/-20). Prerequisite: `UserProperties` becomes an
       order-preserving `List<Pair<String, String>>` instead of a `Map` — MQTT-3.3.2-18
       requires forwarding user properties in their original order, which the map keyed by
       name already destroys at parse time.
7. [ ] **CONNACK capability advertisement** — declare Maximum Packet Size (the 8 KB buffer
       limit!), Retain Available, Wildcard/Shared/Subscription-Identifier availability,
       Maximum QoS (done — but per §3.2.2.3.4 only values 0/1 are legal; don't encode it if
       config ever allows QoS 2). Compliant clients adapt automatically; today they find
       limits by being disconnected.
8. [ ] **Assigned Client Identifier** — clients connecting with an empty client id are
       common; server must generate and return one (§3.1.3.1).
9. [ ] **Protocol version/name validation** — reject MQTT 3.1.1 clients cleanly with
       UNSUPPORTED_PROTOCOL_VERSION instead of a parse explosion (3.1.1 has no properties
       field, so today it misparses). Actually *supporting* 3.1.1 would be the single biggest
       real-world compat win, but it forks the parser — decide deliberately, not by accident.

Consciously deferred (oddly specific / QoS>0 territory): topic aliases, receive maximum flow
control (arrives with QoS 1 as the in-flight window, Phase 6), request/response info, payload
format validation, shared subscriptions, AUTH/enhanced auth, session state & expiry (the
`Session` object exists since Phase 3.2; the full expiry/queueing semantics arrive with
QoS 1 in Phase 6).

## Phase 5 — Telemetry & metrics

- [ ] Define a tiny `MetricsSink` interface in `logic` (matches the existing style —
      `PayloadManager` is the same trick) so `logic` stays dependency-free; implement it in
      `server` with **Micrometer + Prometheus registry**, exposed via the ktor server that is
      already a dependency (`/metrics`, plus a trivial `/health`).
- [ ] Instrument the points that describe broker health:
      - connections: active gauge, total/failed counters, disconnect reasons by code
      - packets in/out counters by type; bytes in/out
      - publish fan-out: subscribers-per-publish distribution, publish→last-write latency
      - **command channel depth** — the single best saturation signal for the actor design
      - buffer pool: allocated/leased gauges (already tracked as AtomicIntegers, just expose)
      - parse errors, keep-alive timeouts, session takeovers
- [ ] docker-compose with Prometheus + Grafana + a starter dashboard.
- [ ] **Load-testing harness** (emqtt-bench or mqtt-bench in a container): connect N
      clients, publish at rate R, watch the dashboard. This is also the measurement rig that
      Phases 6 and 7 depend on — perf claims need numbers.
- [ ] **APM / tracing / profiling** (after metrics are in, not instead of them):
      - **Tracing**: OpenTelemetry with manual spans through the packet pipeline —
        read → parse → actor queue wait → handle → fan-out → per-subscriber write. The
        custom TCP protocol gets nothing from auto-instrumentation, so this is hand-placed
        spans at the module seams (the observer/`MetricsSink` interface is a natural carrier
        for span context). Export to Grafana Tempo or Jaeger in the compose stack. Sample
        (e.g. 1-in-N packets) — a span per packet at full load measures the tracer, not the
        broker. The killer chart: "where did this publish spend its microseconds," especially
        actor-queue wait time under load.
      - **Continuous profiling**: JFR (built into the JVM, ~free overhead, records
        allocations/locks/GC — directly measures the zero-copy claims) viewed in JDK Mission
        Control; or Pyroscope in the compose stack for flame graphs over time next to the
        Grafana dashboards.
      - Sequencing note: metrics answer "is it healthy," traces answer "where does the time
        go," profiles answer "which code allocates/burns CPU." Build in that order — each
        one tells you where to point the next.

Caveat: Micrometer is JVM-only. The `MetricsSink` indirection is exactly what keeps a later
native experiment possible without ripping instrumentation out of `logic`.

## Phase 6 — QoS 1 (then decide on QoS 2)

Everything here assumes Phase 3's structure: session state is actor-owned, no new
concurrency model appears.

- [ ] **`PayloadHandle` seam + copy-at-enqueue** per the decision recorded in Phase 3: one
      copy (payload + topic) into right-sized pooled buffers when a message enters the
      unacked/queued store, read lease released immediately. QoS 0 fan-out path untouched.
- [ ] **Session state machine**: packet-id allocator, unacked map, Receive Maximum window
      (supersedes the Phase 3.3 drop policy as the in-flight bound for QoS 1 traffic),
      offline queueing bounded by session expiry + message expiry.
- [ ] **PUBACK handling**; retransmission on reconnect with DUP=1 (encoders parameterized
      since Phase 3.1); per-client ordering proof (MQTT-4.6) — actor order + writer FIFO
      already provide it, write the test that pins it down.
- [ ] Done when: kill -9 a subscriber mid-stream, reconnect with `cleanStart=0`, and every
      unacked message arrives exactly once at application level with DUP set correctly;
      `@MandatoryNormativeStatementTest` coverage for the MQTT-4.3.2 statements.
- [ ] **QoS 2 afterwards** is the same shape with a bigger state machine (PUBREC/PUBREL/
      PUBCOMP both directions + inbound packet-id dedup) — decide by appetite after QoS 1
      ships, not now.

Perf follow-up parked behind the Phase 5 rig: pin-while-in-flight / copy-on-eviction inside
`PayloadHandle` (zero-copy QoS 1 for the happy path), `Topic`/`TopicFilter` allocation diet
(`LazyThreadSafetyMode.NONE` or an iterative segment cursor, no list concat per tree node),
a `Segment` wrapper for tree keys.

## Phase 7 — Native: measure first, then decide

Honest assessment of Kotlin/Native for this codebase:

- **The core design is built on `java.nio.ByteBuffer`** — Topic/TopicFilter, the whole
  parser/encoder, BufferPool, the zero-copy scheme. K/N has no ByteBuffer; everything moves
  to kotlinx-io / ktor `Memory`. That is a rewrite of the performance-critical heart, not a
  port. Also JVM-only: ThreadLocal CharsetDecoder, java.util.concurrent, mockk (tests),
  ktor-network-tls (server TLS on native is effectively unavailable).
- **The performance assumption deserves scrutiny**: for long-running, throughput-bound
  workloads, JVM JIT typically *beats* Kotlin/Native AOT (K/N's GC and codegen are younger).
  What native genuinely wins is startup time, memory footprint (no JVM), and single-binary
  deployment — deployment ergonomics, not necessarily broker throughput.

Suggested path:

1. [ ] Establish a JVM performance baseline with the Phase 5 load rig (throughput, p99
       latency, RSS). No baseline → no way to know if native "gained" anything.
2. [ ] Cheap experiment: **GraalVM native-image** on the existing JVM code. Gives the
       startup/memory/single-binary wins with days of effort (mostly build config; the code
       uses almost no reflection) instead of a rewrite. Compare against baseline.
3. [ ] Only if numbers and appetite still point to K/N: introduce an own buffer abstraction
       over ByteBuffer first (multiplatform-ready seam, and a nice OOP exercise in its own
       right), migrate module by module behind green tests — packets → parser → logic —
       leaving `server` JVM until ktor-network-on-native is proven for this use case.

## Dependency sketch

```
Phase 1 (tests) ──► everything else
Phase 2 (CI/Docker) ──► Phase 5 compose stack, Phase 7 experiments
Phase 3 (architecture) ──► Phase 4 (features land once), Phase 6 (QoS 1 state)
Phase 3.1 (header flags) ──► retain (4.3), QoS 1 (Phase 6)
Phase 5 (load rig) ──► Phase 6 (verify under load), Phase 7 (baseline before native)
```
