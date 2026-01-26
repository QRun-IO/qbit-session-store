# QBit Session Store

## Overview

The QBit Session Store provides enterprise-grade session management for QQQ applications. It solves the fundamental challenge of maintaining user sessions across horizontally-scaled deployments, container restarts, and blue-green deployments.

## Why Session Store?

Modern applications require:

- **Horizontal scaling**: Load balancers distribute requests across multiple instances
- **Zero-downtime deployments**: Users shouldn't be logged out during releases
- **High availability**: Session data must survive instance failures
- **Performance**: Sub-millisecond session lookups at scale

The QBit Session Store addresses all of these with a pluggable architecture that lets you choose the right backend for your needs.

## Storage Providers

### Redis Provider

**Best for:** Production deployments requiring high availability and horizontal scaling.

Redis provides sub-millisecond session lookups with native TTL expiration. The provider uses connection pooling, supports authentication, and automatically uses Redis 6.2+ atomic operations when available.

```java
new QSessionStoreQBitConfig()
   .withProviderType(QSessionStoreProviderType.REDIS)
   .withRedisHost("redis.example.com")
   .withRedisPort(6379)
   .withRedisPassword("secure-password")
   .withRedisKeyPrefix("myapp:session:")
   .withDefaultTtl(Duration.ofHours(8))
   .withEnableSlidingExpiration(true)
```

**Features:**
- Connection pooling (128 max connections)
- Automatic reconnection
- Native TTL expiration (no cleanup job needed)
- Atomic `GETEX` for load-and-touch operations (Redis 6.2+)
- Namespace isolation via key prefixes

### Table-Based Provider

**Best for:** Multi-instance deployments without external dependencies.

Uses a QQQ table for session persistence, leveraging your existing database infrastructure. Includes automatic cleanup of expired sessions.

```java
new QSessionStoreQBitConfig()
   .withProviderType(QSessionStoreProviderType.TABLE_BASED)
   .withBackendName("primaryBackend")
   .withTableName("storedSession")
   .withTableNamePrefix("myapp_")
   .withDefaultTtl(Duration.ofHours(8))
   .withEnableCleanupProcess(true)
   .withCleanupIntervalSeconds(300)
```

**Features:**
- No external dependencies
- Uses existing database infrastructure
- Automatic expired session cleanup
- Works with any QQQ-supported database

### In-Memory Provider

**Best for:** Development, testing, and single-instance deployments.

Fast ConcurrentHashMap-based storage with LRU eviction when capacity is reached.

```java
new QSessionStoreQBitConfig()
   .withProviderType(QSessionStoreProviderType.IN_MEMORY)
   .withMaxCacheSize(10000)
   .withDefaultTtl(Duration.ofMinutes(30))
```

**Features:**
- Zero configuration
- LRU eviction policy
- No external dependencies

### Custom Provider

Implement `QSessionStoreProviderInterface` for specialized backends (Memcached, Hazelcast, etc.).

```java
new QSessionStoreQBitConfig()
   .withProviderType(QSessionStoreProviderType.CUSTOM)
   .withCustomProviderCodeReference(new QCodeReference(MyCustomProvider.class))
```

## Configuration Reference

### Common Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `providerType` | Enum | (required) | `IN_MEMORY`, `TABLE_BASED`, `REDIS`, or `CUSTOM` |
| `defaultTtl` | Duration | 1 hour | Session expiration time |
| `enableSlidingExpiration` | Boolean | true | Reset TTL on each session access |
| `enableCleanupProcess` | Boolean | true | Register the cleanup scheduler process |
| `cleanupIntervalSeconds` | Integer | 300 | Cleanup process run interval |

### Redis Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `redisHost` | String | (required) | Redis server hostname |
| `redisPort` | Integer | 6379 | Redis server port |
| `redisPassword` | String | null | Redis AUTH password |
| `redisKeyPrefix` | String | `qqq:session:` | Key namespace prefix |

### Table-Based Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `backendName` | String | (required) | QQQ backend name for the session table |
| `tableName` | String | `storedSession` | Name of the session table |
| `tableNamePrefix` | String | null | Prefix for table names (e.g., `myapp_`) |

### In-Memory Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `maxCacheSize` | Integer | 10000 | Maximum sessions before LRU eviction |

## Integration

### Basic Setup

```java
import com.kingsrook.qbits.sessionstore.*;
import java.time.Duration;

public class MyAppMetaDataProducer
{
   public void produce(QInstance qInstance)
   {
      // Configure session store
      new QSessionStoreQBitProducer()
         .withConfig(new QSessionStoreQBitConfig()
            .withProviderType(QSessionStoreProviderType.REDIS)
            .withRedisHost(System.getenv("REDIS_HOST"))
            .withDefaultTtl(Duration.ofHours(8)))
         .produce(qInstance, "sessionStore");

      // ... rest of your metadata
   }
}
```

### How Sessions Flow

1. **User logs in**: Authentication module creates `QSession`, QBit stores it in Redis
2. **Subsequent requests**: Middleware loads session from Redis, optionally resets TTL
3. **User activity**: Sliding expiration keeps active users logged in
4. **Session timeout**: Redis automatically expires inactive sessions
5. **User logs out**: Session is explicitly removed from Redis

### Database Schema (Table-Based)

When using TABLE_BASED provider, create this table in your database:

```sql
CREATE TABLE stored_session (
   id SERIAL PRIMARY KEY,
   session_uuid VARCHAR(36) NOT NULL UNIQUE,
   user_id VARCHAR(255),
   session_data TEXT NOT NULL,
   expires_at TIMESTAMP NOT NULL,
   create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stored_session_uuid ON stored_session(session_uuid);
CREATE INDEX idx_stored_session_expires ON stored_session(expires_at);
```

## Architecture

The QBit follows QQQ's architectural principles:

- **Strategy Pattern**: Swap providers without code changes
- **Registry Pattern**: Providers register with `QSessionStoreRegistry` in core
- **Interface in Core**: `QSessionStoreProviderInterface` defined in `qqq-backend-core`
- **Implementation as QBit**: Providers packaged as optional dependency

This separation ensures core QQQ never depends on optional backends like Redis.

## Performance Considerations

### Redis

- Use connection pooling (default: 128 connections)
- Deploy Redis close to application servers (same AZ/region)
- Enable Redis persistence (RDB/AOF) for durability
- Consider Redis Cluster for datasets > 25GB

### Table-Based

- Index `session_uuid` and `expires_at` columns
- Run cleanup process during off-peak hours for large session counts
- Consider partitioning the table by `expires_at` for high-volume applications

### Sizing

| Sessions | Recommended Provider |
|----------|---------------------|
| < 1,000 | In-Memory or Table-Based |
| 1,000 - 100,000 | Table-Based or Redis |
| > 100,000 | Redis or Redis Cluster |

## Requirements

- QQQ Backend Core 0.40.0+
- Java 21+
- Redis 6.0+ (6.2+ recommended for `GETEX` support)

## License

Apache License 2.0

## Support

- [GitHub Issues](https://github.com/QRun-IO/qbit-session-store/issues)
- [QQQ Discussions](https://github.com/QRun-IO/qqq/discussions)
