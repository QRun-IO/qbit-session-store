# CLAUDE.md - QBit Session Store

## Overview

QBit providing pluggable session storage/caching for QQQ applications. Implements the Strategy pattern for easy extensibility with three built-in providers.

**Current Version:** 0.1.0-SNAPSHOT
**License:** Apache-2.0

## Build Commands

```bash
mvn clean compile           # Compile
mvn clean test              # Run unit tests
mvn clean verify            # Build + tests + coverage
mvn clean install           # Install to local repo
```

## Project Structure

```
src/main/java/com/kingsrook/qbits/sessionstore/
├── QSessionStoreProviderInterface.java   # Strategy interface
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
      .withProviderType(QSessionStoreProviderType.TABLE_BASED)
      .withBackendName("primaryBackend")
      .withDefaultTtl(Duration.ofHours(8))
      .withEnableSlidingExpiration(true))
   .produce(qInstance);
```

## Configuration Options

| Field | Default | Description |
|-------|---------|-------------|
| providerType | required | IN_MEMORY, TABLE_BASED, REDIS, or CUSTOM |
| defaultTtl | 1 hour | Session expiration duration |
| enableSlidingExpiration | true | Reset TTL on access |
| backendName | - | Required for TABLE_BASED |
| tableName | "storedSession" | Table name for TABLE_BASED |
| maxCacheSize | 10000 | LRU size for IN_MEMORY |
| redisHost | - | Required for REDIS |
| redisPort | 6379 | Redis port |
| redisKeyPrefix | "qqq:session:" | Redis key namespace |

## Dependencies

- `qqq-backend-core` (required)
- `redis.clients:jedis` (optional, for Redis provider)

## Related

- GitHub Issue: QRun-IO/qqq#336
- Design Plan: `/Users/james.maes/Git.Local/qrun/qqq/docs/PLAN-session-store-qbit.md`
