# Traffic Parrot Demo: SFTP + FIX 4.4

Two small Java applications that exchange FIX 4.4 files over SFTP. Used to demonstrate
Traffic Parrot's ability to virtualize SFTP-based interfaces — record real traffic,
replay canned responses, and serve dynamic responses templated from request fields.

## Prerequisites

- Java 21
- Maven 3.9+ (or use `./mvnw` if you copy a wrapper in)

## Build

```bash
cd fix-sftp-fix
mvn clean package
```

Produces:

- `fix-sftp-server/target/fix-sftp-server.jar`
- `fix-sftp-client/target/fix-sftp-client.jar`

## Run

### 1. Sanity check (no Traffic Parrot)

```bash
java -jar fix-sftp-server/target/fix-sftp-server.jar &
java -jar fix-sftp-client/target/fix-sftp-client.jar &
```

Open <http://localhost:8082>, fill in the form (defaults work), click
**Send NewOrderSingle**. The form uploads the request to `inbound/` on the
SFTP server; the server's business loop generates an `ExecutionReport` into
`outbound/` and the client UI displays it.

The server's UI at <http://localhost:8081> shows live SSH sessions and the contents
of `inbound/` and `outbound/` — its real interface. It is deliberately unaware of
any proxy staging directories that may live on the same filesystem.

### 2. Through Traffic Parrot — record mode

In a proxied run, the client is reconfigured to use a different pair of directories
(`tp-inbound/` and `tp-outbound/`) so TP can sit between the client and the real
server without a consumer race. TP forwards `tp-inbound/ → inbound/` (the real
server processes the order) and `outbound/ → tp-outbound/` (the response is
forwarded back to the client). All traffic is recorded as a stub mapping in TP.

The proxy staging dirs are not part of the server's interface — they are created by
the orchestrator before TP starts, on the same SFTP filesystem the server happens
to host. The server itself never lists or acknowledges them.

Use the orchestrator script in the `trafficparrot-service-virtualization` repo
(`scripts/demo/sftp-fix/run-demo.sh`) to wire this up automatically.

### 3. Through Traffic Parrot — replay mode

The real server can be stopped or kept idle. TP serves the recorded mapping (canned
or dynamically-templated) when the client posts to `tp-inbound/`.

## Configuration

Defaults are sensible for a single-host demo. Override via CLI flags or env vars:

| Server flag        | Env var       | Default                |
|--------------------|--------------|------------------------|
| `--sftp-port N`    | `SFTP_PORT`   | 2222                   |
| `--ui-port N`      | `UI_PORT`     | 8081                   |
| `--root DIR`       | `SFTP_ROOT`   | `build/sftp-root`      |
| `--user U`         | `SFTP_USER`   | `demo`                 |
| `--password P`     | `SFTP_PASSWORD` | `demo`               |
| `--no-process`     | `NO_PROCESS`  | (disables business loop, useful for replay-only) |

| Client flag           | Env var      | Default     |
|-----------------------|-------------|-------------|
| `--ui-port N`         | `UI_PORT`    | 8082        |
| `--sftp-host H`       | `SFTP_HOST`  | `localhost` |
| `--sftp-port N`       | `SFTP_PORT`  | 2222        |
| `--user U`            | `SFTP_USER`  | `demo`      |
| `--password P`        | `SFTP_PASSWORD` | `demo`   |
| `--request-dir D`     | `REQUEST_DIR`  | `inbound`  |
| `--response-dir D`    | `RESPONSE_DIR` | `outbound` |

## File layout on the SFTP server

The server exposes two directories — its real interface:

```
<SFTP root>/
├── inbound/      ← server polls here
└── outbound/     ← server writes ExecutionReport here
```

When the demo is driven through Traffic Parrot, the orchestrator additionally
creates a pair of TP-side staging directories on the same filesystem:

```
<SFTP root>/
├── tp-inbound/   ← client uploads here; TP polls and forwards to inbound/
└── tp-outbound/  ← TP writes responses here; client polls
```

These belong to TP, not the server. The server's UI does not list them.

## Notes

- This is demo code. There is no FIX session layer (Logon/Heartbeat) — files are
  exchanged "ad hoc" as is common for batch FIX-over-file integrations. The
  ExecutionReport produced by the real server has correct BodyLength and CheckSum.
- The server uses Apache MINA SSHD with a generated host key. Each fresh start
  generates a new host key — don't be surprised if your client's known_hosts
  complains; the `fix-sftp-client` ships with `StrictHostKeyChecking=no`.
- License: MIT (matches the parent repo).
