# Daily Build: Session Store QBit

Been working on something I've needed for a while now - pluggable session storage for QQQ.

The backstory: we've had a few projects where we needed to run multiple instances behind a load balancer, and sessions were a pain point. User logs in, hits instance A, session lives in memory there. Next request goes to instance B, and they're logged out. Classic problem.

So I built a QBit for it. Three providers out of the box:

- **IN_MEMORY** - what we already had, basically. Good for dev.
- **TABLE_BASED** - stores sessions in a QQQ table. Works if you don't want to spin up Redis.
- **REDIS** - the real solution for production HA stuff.

The Redis one was interesting to build. Jedis makes it pretty straightforward, but I wanted to optimize for the common case - load a session and reset its TTL in one shot. Redis 6.2 added `GETEX` which does exactly that atomically. Older versions fall back to GET + EXPIRE.

Usage is simple:

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.REDIS)
      .withRedisHost("localhost")
      .withDefaultTtl(Duration.ofHours(8)))
   .produce(qInstance, "sessionStore");
```

The QBit registers itself with the new `QSessionStoreRegistry` in core, so authentication just picks it up automatically. No changes needed to your auth code.

One thing I'm happy with architecturally - this follows the pattern we established with other optional modules. Core defines the interface, the QBit provides implementations and registers on startup. Core never knows about Redis or Jedis. Clean separation.

Still need to write more tests (only have InMemory and config tests so far), but the implementation is solid. Been running it locally and it works as expected.

Next up: Testcontainers for Redis integration tests, then probably cut a 0.1.0 release.

## Try It

Add to your `pom.xml`:

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-session-store</artifactId>
   <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Then in your QInstance setup, add the producer before your auth config. That's it - sessions now go to Redis instead of memory.

---

**Repo:** [QRun-IO/qbit-session-store](https://github.com/QRun-IO/qbit-session-store)

**Commits:**
- [698a3ca](https://github.com/QRun-IO/qbit-session-store/commit/698a3ca) - initial implementation
- [3574a52](https://github.com/QRun-IO/qbit-session-store/commit/3574a52) - core registry integration
