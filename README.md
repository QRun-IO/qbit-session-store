# QBit Session Store

Pluggable session storage/caching for QQQ applications. Avoids re-deriving expensive session data (security keys, permissions) on every request.

## Providers

- **InMemory** - ConcurrentHashMap with LRU eviction. Best for dev/testing.
- **TableBased** - QQQ table storage. Best for multi-instance persistence.
- **Redis** - Distributed caching with native TTL. Best for HA deployments.

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

## License

Apache-2.0 - See [LICENSE](LICENSE)
