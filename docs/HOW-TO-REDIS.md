# How-To: Implement Redis Session Storage in Your QQQ Application

This guide walks through integrating the QBit Session Store with Redis into an existing QQQ application.

## Prerequisites

- QQQ application (0.40.0+)
- Redis server (6.2+ recommended for optimal performance)
- Maven-based project

## Step 1: Add the Dependency

Add to your `pom.xml`:

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-session-store</artifactId>
   <version>0.1.0</version>
</dependency>
```

The Jedis Redis client is included transitively.

## Step 2: Configure the QBit

In your `QInstance` setup (typically in your application's MetaData producer or initialization code):

```java
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderType;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitProducer;
import java.time.Duration;

// In your QInstance initialization
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.REDIS)
      .withRedisHost("localhost")
      .withRedisPort(6379)
      .withDefaultTtl(Duration.ofHours(8))
      .withEnableSlidingExpiration(true))
   .produce(qInstance, "sessionStore");
```

## Step 3: Configure Redis Connection (Production)

For production deployments with authentication:

```java
new QSessionStoreQBitConfig()
   .withProviderType(QSessionStoreProviderType.REDIS)
   .withRedisHost(System.getenv("REDIS_HOST"))
   .withRedisPort(Integer.parseInt(System.getenv("REDIS_PORT")))
   .withRedisPassword(System.getenv("REDIS_PASSWORD"))
   .withRedisKeyPrefix("myapp:session:")
   .withDefaultTtl(Duration.ofHours(8))
   .withEnableSlidingExpiration(true)
```

## Configuration Reference

| Option | Default | Description |
|--------|---------|-------------|
| `redisHost` | (required) | Redis server hostname |
| `redisPort` | 6379 | Redis server port |
| `redisPassword` | null | Redis AUTH password |
| `redisKeyPrefix` | `qqq:session:` | Key namespace prefix |
| `defaultTtl` | 1 hour | Session expiration time |
| `enableSlidingExpiration` | true | Reset TTL on each access |
| `enableCleanupProcess` | true | Register cleanup process (no-op for Redis) |

## How It Works

Once configured, QQQ's authentication system automatically uses the session store:

1. **Login**: Session is serialized to JSON and stored in Redis with TTL
2. **Request**: Session is loaded from Redis; TTL is reset if sliding expiration is enabled
3. **Logout**: Session is removed from Redis

Redis keys follow the pattern: `{prefix}{sessionUuid}` (e.g., `qqq:session:abc-123-def`)

## Verifying the Integration

Check Redis directly:

```bash
redis-cli
> KEYS qqq:session:*
> TTL qqq:session:<uuid>
> GET qqq:session:<uuid>
```

## AWS ElastiCache Example

```java
new QSessionStoreQBitConfig()
   .withProviderType(QSessionStoreProviderType.REDIS)
   .withRedisHost("my-cluster.cache.amazonaws.com")
   .withRedisPort(6379)
   .withRedisKeyPrefix("prod:session:")
   .withDefaultTtl(Duration.ofHours(24))
```

## Troubleshooting

**Connection refused**: Verify Redis is running and accessible from your application host.

**Sessions not persisting**: Check that the QBit producer is called before authentication is initialized.

**Key not found after restart**: Redis persistence may be disabled. Configure RDB or AOF persistence in Redis.

## Next Steps

- Configure Redis Cluster for high availability
- Set up Redis Sentinel for automatic failover
- Monitor session metrics via Redis INFO command
