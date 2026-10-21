# Application-scoped session store registration

The candidate core registry resolves the QInstance in QContext. Application
startup must register explicitly, even when no request context exists:

```java
QSessionStoreRegistry.getInstance(qInstance).register(provider);
```

Calls to `getInstance()` within a request use that request's application. Without
a QInstance, that call accesses a separate standalone registry. An application
never inherits that standalone provider. Existing hosts that register outside
QContext must migrate to the explicit overload alongside this core candidate.
Deploy matching session-store/core versions; this is not a transparent change for
old context-free startup integrations.

Session-store's producer now supplies its target QInstance to both core registration
and qbit context. Context getters/setters/clear operate on the current application,
with explicit QInstance overloads for metadata production/background operations.
Health metadata selects its target application and the health probe uses the
QInstance in HealthCheckInput (falling back to current context only if omitted).

Regression reproduced before the fix: after application A stored a session,
unconfigured application B loaded that same session through QSessionStoreHelper.
The test now checks independent same-ID sessions, isolation of clearing, and health
checks for an explicit target while a different application is on the caller thread.

Hosts sharing a Redis server must configure separate application/authentication
domain namespaces. The `SHARED_REDIS` provider uses those explicit settings and
the owning Redis QBit's pool. Its runtime releases owned registrations on stop
and reconstructs them on restart. Local acceptance covers launcher lifecycle,
HTTP authentication/logout and Redis outage/retry; coordinated published-artifact
and clean-host verification remain release gates. See the shared Redis setup in
[README](../README.md).
