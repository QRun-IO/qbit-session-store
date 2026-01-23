# QBit Session Store

Pluggable session storage/caching for QQQ applications. Avoids re-deriving expensive session data (security keys, permissions) on every request.

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

### Custom Provider

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.CUSTOM)
      .withCustomProviderCodeReference(new QCodeReference(MyCustomProvider.class)))
   .produce(qInstance);
```

## Accessing the Provider

After producing the QBit, retrieve the provider via context:

```java
QSessionStoreProviderInterface provider = QSessionStoreQBitContext.getProvider();

// Store a session
provider.store(session.getUuid(), session, Duration.ofHours(1));

// Load a session
Optional<QSession> cached = provider.load(sessionUuid);

// Touch (refresh TTL)
provider.touch(sessionUuid);

// Remove
provider.remove(sessionUuid);
```

## License

Apache-2.0 - See [LICENSE](LICENSE)
