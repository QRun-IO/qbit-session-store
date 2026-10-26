# Shared sessions during controlled Redis promotion

Tracks [QQQ #1301](https://github.com/QRun-IO/qqq/issues/1301). This is a test of
the actual shared session provider, complementary to the connection and Pub/Sub
promotion fixture in Redis draft PR4. No production provider behavior changes.

`SharedRedisSessionStoreProviderIT.replicatedSessionsAndRevocationsSurviveControlledPromotion`
uses a real Redis primary/replica pair and three separately configured QQQ
instances. One writes to the primary; two use the replica endpoint, with fixed
and sliding expiry settings. The test verifies:

- Session data reaches the replica and writes to the read-only replica fail.
- A removed session is absent after a subsequent write reaches the replica.
  This observation establishes that this test's deletion was replicated.
- After the old primary stops, its provider raises an infrastructure failure
  rather than reporting a normal session miss.
- After promotion, the original session remains readable and the replicated
  revocation remains absent. Touch/read-and-touch cannot recreate it.
- New sessions, cross-node deletion, sliding expiry and natural expiry work on
  the promoted endpoint. Touch cannot recreate an expired session.

Verification passed on October 26, 2026: 67 unit tests and 30 integration tests,
no failures/errors/skips, zero Checkstyle violations, and coverage gates passed.
The first full run encountered a replica read timeout during initial loading;
the fixture now retries connection failures only inside its bounded 15-second
initial replication wait. Post-startup assertions and runtime timeouts are
unchanged. The focused test also passed before that startup adjustment.
Containers, network, provider registrations and runtime pools belong to the
fixture and are cleaned up by it. The existing Maven wrapper from the core
checkout can run this repository's POM on Windows, where this checkout has no
`mvnw.cmd`.

## Limits and remaining qualification

The test deliberately waits for replication before stopping the primary. It
does not prove that unreplicated session creation, permission changes or logout
survive failover. Redis replication is asynchronous; recovered connectivity
alone does not establish durable authentication state. See the official
[replication documentation](https://redis.io/docs/latest/operate/oss_and_stack/management/replication/).

Before production failover support, qualify the actual host endpoint routing,
old-primary fencing, TLS/authentication, and the policy for lost or restored
authentication state. In particular, a logout lost before replication can leave
an older session copy on the promoted node; this fixture must not be cited as
proof that such a session cannot return. Presence generation rotation alone
does not invalidate session-store data.

This uses an unchanged explicit replica endpoint and controlled promotion. It
does not implement native Sentinel discovery, Cluster routing or automatic
application reconfiguration, and does not qualify a deployed Kof22 cluster.
