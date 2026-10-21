# QBit Session Store

Pluggable session storage/caching for QQQ applications. Avoids re-deriving expensive session data (security keys, permissions) on every request.

## Why This Exists

Every QQQ request authenticates and builds a `QSession` containing security keys, permissions, and user metadata. For backends like Auth0, that means a network round-trip on every request. This QBit lets you store and reuse sessions across requests with configurable TTL, sliding expiration, and multiple storage backends.

## Project Status

**0.1.0-SNAPSHOT** -- under active development. API may change before 1.0.

## Features

- Four storage backends: in-memory (LRU), QQQ table, Redis, or custom
- Sliding expiration resets TTL on access
- Scheduled cleanup process for expired sessions
- Declarative QBit configuration via `QSessionStoreQBitProducer`
- Thread-safe across all providers

## Providers

| Provider | Storage | Best For |
|----------|---------|----------|
| IN_MEMORY | ConcurrentHashMap + LRU | Dev, testing, single-instance |
| TABLE_BASED | QQQ table (StoredSession) | Multi-instance, persistence |
| REDIS | Jedis with native TTL | Distributed HA deployments |
| CUSTOM | User-provided implementation | Specialized backends |

## Quick Start

Add the dependency to your project:

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-session-store</artifactId>
   <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Produce the QBit into your QInstance:

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.TABLE_BASED)
      .withBackendName("primaryBackend")
      .withDefaultTtl(Duration.ofHours(8))
      .withEnableSlidingExpiration(true))
   .produce(qInstance);
```

## Configuration Options

| Field | Default | Description |
|-------|---------|-------------|
| `providerType` | (required) | IN_MEMORY, TABLE_BASED, REDIS, or CUSTOM |
| `defaultTtl` | 1 hour | Session expiration duration |
| `enableSlidingExpiration` | true | Reset TTL on access |
| `backendName` | - | Required for TABLE_BASED |
| `tableName` | "storedSession" | Table name for TABLE_BASED |
| `tableNamePrefix` | - | Prefix for table name (e.g., "app_") |
| `maxCacheSize` | 10000 | LRU size for IN_MEMORY |
| `redisHost` | - | Required for REDIS |
| `redisPort` | 6379 | Redis port |
| `redisPassword` | - | Redis password (optional) |
| `redisKeyPrefix` | "qqq:session:" | Redis key namespace |
| `enableCleanupProcess` | true | Enable scheduled cleanup |
| `cleanupIntervalSeconds` | 300 | Cleanup interval (5 min) |

## Table Schema (TABLE_BASED Provider)

The QBit automatically creates the table metadata. You must create the physical table in your database:

```sql
-- PostgreSQL
CREATE TABLE stored_session (
   id SERIAL PRIMARY KEY,
   session_uuid VARCHAR(36) NOT NULL UNIQUE,
   user_id VARCHAR(255),
   session_data TEXT,
   expires_at TIMESTAMP NOT NULL,
   create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- With table prefix (e.g., tableNamePrefix="app_")
CREATE TABLE app_stored_session (
   -- same columns as above
);
```

**Column mapping:**
- `id` - Auto-increment primary key
- `session_uuid` - UUID identifying the session (unique index)
- `user_id` - User ID reference for auditing
- `session_data` - Serialized QSession as JSON
- `expires_at` - Session expiration timestamp
- `create_date` / `modify_date` - Audit timestamps

## Usage Examples

### In-Memory (Development)

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.IN_MEMORY)
      .withMaxCacheSize(1000)
      .withDefaultTtl(Duration.ofMinutes(30)))
   .produce(qInstance);
```

### Table-Based with Prefix

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.TABLE_BASED)
      .withBackendName("primaryBackend")
      .withTableNamePrefix("myapp_")  // Results in "myapp_storedSession" table
      .withDefaultTtl(Duration.ofHours(8)))
   .produce(qInstance);
```

### Redis

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.REDIS)
      .withRedisHost("redis.example.com")
      .withRedisPort(6379)
      .withRedisPassword("secret")
      .withRedisKeyPrefix("myapp:session:")
      .withDefaultTtl(Duration.ofHours(4)))
   .produce(qInstance);
```

### Shared Redis infrastructure

Use `SHARED_REDIS` when the application installs `qbit-redis`. Include that optional
artifact explicitly in the host and produce its named connection metadata before
session-store. The Redis runtime owns TLS, credentials, bounded pools and health.
Session-store owns JSON serialization, session expiry and payload limits.

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.SHARED_REDIS)
      .withSharedRedis(new SharedRedisSessionStoreSettings(
         "sessions", "production", "orders-app", "authentication", 65536))
      .withDefaultTtl(Duration.ofHours(1))
      .withEnableSlidingExpiration(true))
   .produce(qInstance);
```

Settings are connection name, deployment, application, authentication-domain scope,
and maximum serialized bytes. Nodes serving the same application/domain must use
identical scope; distinct applications/domains must use distinct scope. Resolve it
from host configuration, never an unauthenticated request. Redis namespacing encodes
each segment independently. This provider does not interpret per-user tenant claims.

Operations require the owning Redis runtime to be started. The provider never opens
or closes a pool and never falls back to another connection. `loadAndTouch` uses
atomic GETEX when sliding is enabled, plain GET otherwise; Redis 6.2+ is required.
Missing values are cache misses; infrastructure, size and decoding failures throw.
Failed remove also throws so logout can retry. TTLs must be 1 ms through 365 days;
serialized payload bounds may be configured up to 1 MiB. No cleanup job is produced
because Redis owns expiry. Framework Redis and session-store health checks report
availability; the provider's status string is only descriptive.

The existing `REDIS` provider retains its independent legacy pool/configuration.
Selecting `SHARED_REDIS` is explicit and uses a new namespace; it does not migrate
existing cache entries. A framework runtime service releases its owned registry/context entries in
`afterApplicationStop`, after dependent services have stopped; it never closes
the Redis pool. Repeated cleanup is harmless and preserves replacement providers.
Restart reconstructs registration from metadata after Redis starts. Manual hosts
must honor the same runtime callbacks. Shared providers weakly reference their
application so an abandoned pre-start registration cannot retain the owner.

Local acceptance covers real HTTP authentication/logout and Redis outage/retry
through the core sample's `redis-acceptance` profile. Coordinated artifact
publication and clean host adoption remain release gates. Redis failover topology
qualification is future work tracked in [QQQ #1301](https://github.com/QRun-IO/qqq/issues/1301).

### Custom Provider

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.CUSTOM)
      .withCustomProviderCodeReference(new QCodeReference(MyCustomProvider.class)))
   .produce(qInstance);
```

## Accessing the Provider

After producing the QBit, retrieve the provider for its owning application.
Within a request, `getProvider()` uses `QContext.getQInstance()`. Startup and
background callers should use the explicit overload below. The qbit automatically
registers against the QInstance supplied to `produce`; it never installs a
JVM-wide fallback for other applications.

Hosts registering directly with core must use
`QSessionStoreRegistry.getInstance(qInstance).register(provider)` outside request
context. See [registration migration](docs/SESSION-REGISTRATION.md).

```java
QSessionStoreProviderInterface provider = QSessionStoreQBitContext.getProvider(qInstance);

// Store a session
provider.store(session.getUuid(), session, Duration.ofHours(1));

// Load a session
Optional<QSession> cached = provider.load(sessionUuid);

// Touch (refresh TTL)
provider.touch(sessionUuid);

// Remove
provider.remove(sessionUuid);
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

Apache-2.0 - See [LICENSE](LICENSE)

Launcher acceptance covers normal shutdown, failure of a later runtime, and Redis
authentication failure before session-runtime startup followed by retry. In that
early failure case no Redis pool is published; the weak pre-start registration
remains configured but cannot execute session operations until Redis starts.
