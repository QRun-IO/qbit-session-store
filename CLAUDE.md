# CLAUDE.md - QBit Session Store

## Overview

QBit providing pluggable session storage/caching for QQQ applications. Implements the Strategy pattern for easy extensibility with three built-in providers.

**Current Version:** 0.1.0-SNAPSHOT
**License:** Apache-2.0
**Status:** Implementation complete, unit tested, documented

## Session Continuity

When resuming work, read `docs/SESSION-STATE.md` for current status and next steps.

## Build Commands

```bash
mvn clean compile           # Compile
mvn clean test              # Run unit tests (65 tests)
mvn clean verify            # Build + tests + coverage check
mvn clean install           # Install to local repo
```

## Test Coverage

- **65 unit tests** - all passing
- **62% instruction coverage** (threshold: 60%)
- **80%+ class coverage** (threshold: 80%)
- Provider operation logic (store/load/remove) requires integration tests with real services

## Project Structure

```
src/main/java/com/kingsrook/qbits/sessionstore/
├── QSessionStoreProviderInterface.java   # Strategy interface (extends core)
├── QSessionStoreProviderFactory.java     # Factory for strategy selection
├── QSessionStoreProviderType.java        # Enum: IN_MEMORY, TABLE_BASED, REDIS, CUSTOM
├── QSessionStoreQBitConfig.java          # Configuration
├── QSessionStoreQBitContext.java         # Static context holder
├── QSessionStoreQBitProducer.java        # QBit producer
├── providers/
│   ├── InMemorySessionStoreProvider.java
│   ├── TableBasedSessionStoreProvider.java
│   └── RedisSessionStoreProvider.java
├── model/
│   └── StoredSession.java
├── metadata/
│   ├── StoredSessionTableMetaDataProducer.java
│   └── CleanExpiredSessionsProcessMetaDataProducer.java
└── processes/
    └── CleanExpiredSessionsStep.java

docs/
├── SESSION-STATE.md         # Current session state for continuity
├── HOW-TO-REDIS.md          # Developer guide for Redis integration
├── DAILY-BUILD-POST.md      # Blog post (published to QQQ discussions)
└── MARKETING-SITE.md        # Full documentation for marketing site
```

## Provider Details

| Provider | Storage | Best For |
|----------|---------|----------|
| IN_MEMORY | ConcurrentHashMap + LRU | Dev, testing, single-instance |
| TABLE_BASED | QQQ table (StoredSession) | Multi-instance, persistence |
| REDIS | Jedis with native TTL | Distributed HA deployments |
| CUSTOM | User-provided via QCodeReference | Specialized backends |

## Usage

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.REDIS)
      .withRedisHost("localhost")
      .withDefaultTtl(Duration.ofHours(8))
      .withEnableSlidingExpiration(true))
   .produce(qInstance, "sessionStore");
```

## Configuration Options

| Field | Default | Description |
|-------|---------|-------------|
| providerType | required | IN_MEMORY, TABLE_BASED, REDIS, or CUSTOM |
| defaultTtl | 1 hour | Session expiration duration |
| enableSlidingExpiration | true | Reset TTL on access |
| backendName | - | Required for TABLE_BASED |
| tableName | "storedSession" | Table name for TABLE_BASED |
| tableNamePrefix | - | Prefix for table names (e.g., "app_") |
| maxCacheSize | 10000 | LRU size for IN_MEMORY |
| redisHost | - | Required for REDIS |
| redisPort | 6379 | Redis port |
| redisKeyPrefix | "qqq:session:" | Redis key namespace |
| enableCleanupProcess | true | Register cleanup scheduler |
| cleanupIntervalSeconds | 300 | Cleanup interval (no-op for Redis) |

## Architecture Notes

- **Core integration:** Registers with `QSessionStoreRegistry` in qqq-backend-core
- **Optimized operations:** `loadAndTouch()` combines load + TTL reset in one round-trip
- **Redis 6.2+:** Uses atomic `GETEX` command with fallback for older versions
- **Table-based:** Uses `QSystemUserSession` for internal operations

## Table Schema (TABLE_BASED)

```sql
CREATE TABLE stored_session (
   id SERIAL PRIMARY KEY,
   session_uuid VARCHAR(36) NOT NULL UNIQUE,
   user_id VARCHAR(255),
   session_data TEXT,
   expires_at TIMESTAMP NOT NULL,
   create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Dependencies

- `qqq-backend-core` 0.40.0+ (required)
- `redis.clients:jedis` 5.1.0 (optional, for Redis provider)

## Related

- GitHub Issue: QRun-IO/qqq#336
- Daily Build Post: https://github.com/orgs/QRun-IO/discussions/384
- Repo: https://github.com/QRun-IO/qbit-session-store
