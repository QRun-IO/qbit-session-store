/*
 * Copyright 2024 Kingsrook, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kingsrook.qbits.sessionstore.providers;


import java.time.Duration;
import java.util.List;
import com.kingsrook.qbits.redis.RedisConnectionConfig;
import com.kingsrook.qbits.redis.RedisQBitConfig;
import com.kingsrook.qbits.redis.RedisQBitProducer;
import com.kingsrook.qbits.redis.RedisRuntimeService;
import com.kingsrook.qbits.redis.api.RedisAccess;
import com.kingsrook.qbits.redis.api.RedisNamespace;
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderType;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitContext;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitProducer;
import com.kingsrook.qbits.sessionstore.SharedRedisSessionStoreRuntimeService;
import com.kingsrook.qbits.sessionstore.SharedRedisSessionStoreSettings;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;


/*******************************************************************************
 ** Real shared infrastructure proves namespace, lifecycle and expiry ownership.
 *******************************************************************************/
class SharedRedisSessionStoreProviderIT
{
   /***************************************************************************
    ** Separate nodes share one app's sessions while other apps remain isolated.
    ***************************************************************************/
   @Test
   void sharedPoolsIsolationExpiryAndFailure() throws Exception
   {
      QInstance original = QContext.getQInstance();
      try(GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379))
      {
         redis.start();
         QInstance first = host(redis, "app-a", true);
         QInstance second = host(redis, "app-a", false);
         QInstance other = host(redis, "app-b", true);
         var a = QSessionStoreQBitContext.getProvider(first);
         var b = QSessionStoreQBitContext.getProvider(second);
         var c = QSessionStoreQBitContext.getProvider(other);
         var firstRuntime = new RedisRuntimeService();
         var secondRuntime = new RedisRuntimeService();
         var otherRuntime = new RedisRuntimeService();
         try
         {
            assertThrows(IllegalStateException.class, () -> a.load("session"));
            firstRuntime.start(first);
            secondRuntime.start(second);
            otherRuntime.start(other);
            QContext.setQInstance(other);
            a.store("session", new QSession().withValue("owner", "app-a"), null);
            assertEquals("app-a", b.load("session").orElseThrow().getValue("owner"));
            assertTrue(c.load("session").isEmpty());
            c.store("session", new QSession().withValue("owner", "app-b"), null);
            a.remove("session");
            assertTrue(b.load("session").isEmpty());
            assertEquals("app-b", c.load("session").orElseThrow().getValue("owner"));
            a.touch("session");
            assertTrue(a.loadAndTouch("session").isEmpty());

            var scope = new RedisNamespace("test", "app-a", "authentication", "session-store");
            var client = RedisRuntimeService.connection(first, "sessions").client();
            a.store("sliding", new QSession(), Duration.ofSeconds(1));
            assertTrue(a.loadAndTouch("sliding").isPresent());
            assertTrue(client.pttl(scope.key("sliding")) > 20000);
            a.store("fixed", new QSession(), Duration.ofMillis(200));
            assertTrue(b.loadAndTouch("fixed").isPresent());
            assertTrue(client.pttl(scope.key("fixed")) <= 200);
            long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
            while(b.load("fixed").isPresent() && System.nanoTime() < deadline)
            {
               Thread.sleep(10);
            }
            assertTrue(b.load("fixed").isEmpty());
            assertThrows(IllegalArgumentException.class,
               () -> a.store("bad", new QSession(), Duration.ZERO));
            assertThrows(IllegalArgumentException.class,
               () -> a.store("large", new QSession().withValue("large", "x".repeat(70000)), null));
            RedisAccess.cache(first, "sessions", scope, 65536)
               .put("broken", "test-secret-not-json".getBytes(java.nio.charset.StandardCharsets.UTF_8), Duration.ofSeconds(5));
            var malformed = assertThrows(IllegalStateException.class, () -> a.load("broken"));
            assertFalse(malformed.toString().contains("test-secret"));
            assertNull(malformed.getCause());
            a.cleanExpired();
            assertTrue(a.status().contains("health"));
            assertEquals(Duration.ofSeconds(30), a.getDefaultTtl());

            firstRuntime.stop();
            firstRuntime.afterApplicationStop();
            assertThrows(IllegalStateException.class, () -> a.load("sliding"));
            assertTrue(b.load("sliding").isPresent());
            firstRuntime.start(first);
            assertTrue(a.load("sliding").isPresent());
            redis.stop();
            assertThrows(RuntimeException.class, () -> a.remove("sliding"));
            assertThrows(RuntimeException.class, () -> a.load("sliding"));
         }
         finally
         {
            for(var runtime : List.of(firstRuntime, secondRuntime, otherRuntime))
            {
               runtime.stop();
               runtime.afterApplicationStop();
            }
            for(var instance : List.of(first, second, other))
            {
               QSessionStoreQBitContext.clear(instance);
               QSessionStoreRegistry.getInstance(instance).clear();
            }
            QContext.setQInstance(original);
         }
      }
   }



   /***************************************************************************
    ** Startup failures and shutdown must release only the owning registrations.
    ***************************************************************************/
   @Test
   void registrationLifecycle() throws Exception
   {
      QInstance original = QContext.getQInstance();
      try(GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379))
      {
         redis.start();
         QInstance first = host(redis, "app-a", true);
         QInstance second = host(redis, "app-b", true);
         var firstPool = new RedisRuntimeService();
         var secondPool = new RedisRuntimeService();
         var firstService = new SharedRedisSessionStoreRuntimeService();
         var secondService = new SharedRedisSessionStoreRuntimeService();
         try
         {
            assertThrows(com.kingsrook.qqq.backend.core.exceptions.QException.class,
               () -> firstService.start(first));
            assertNull(QSessionStoreQBitContext.getProvider(first));
            assertTrue(QSessionStoreRegistry.getInstance(first).getProvider().isEmpty());
            firstService.afterApplicationStop();
            firstPool.start(first);
            secondPool.start(second);
            firstService.start(first);
            secondService.start(second);
            var firstProvider = QSessionStoreQBitContext.getProvider(first);
            var secondProvider = QSessionStoreQBitContext.getProvider(second);
            assertNotNull(firstProvider);
            assertThrows(com.kingsrook.qqq.backend.core.exceptions.QException.class,
               () -> firstService.start(first));
            var duplicate = new SharedRedisSessionStoreRuntimeService();
            assertThrows(com.kingsrook.qqq.backend.core.exceptions.QException.class,
               () -> duplicate.start(first));
            duplicate.afterApplicationStop();
            assertSame(firstProvider, QSessionStoreQBitContext.getProvider(first));
            QContext.setQInstance(second);
            firstService.stop();
            assertSame(firstProvider, QSessionStoreQBitContext.getProvider(first));
            firstService.afterApplicationStop();
            firstService.afterApplicationStop();
            assertNull(QSessionStoreQBitContext.getProvider(first));
            assertNull(QSessionStoreQBitContext.getConfig(first));
            assertTrue(QSessionStoreRegistry.getInstance(first).getProvider().isEmpty());
            assertSame(secondProvider, QSessionStoreQBitContext.getProvider(second));
            assertEquals("PONG", RedisRuntimeService.connection(first, "sessions").client().ping());
            firstService.start(first);
            assertNotSame(firstProvider, QSessionStoreQBitContext.getProvider(first));
            var replacement = new InMemorySessionStoreProvider();
            QSessionStoreRegistry.getInstance(first).register(replacement);
            QSessionStoreQBitContext.setProvider(first, replacement);
            firstService.afterApplicationStop();
            assertSame(replacement, QSessionStoreQBitContext.getProvider(first));
            assertSame(replacement, QSessionStoreRegistry.getInstance(first).getProvider().orElseThrow());
            assertThrows(com.kingsrook.qqq.backend.core.exceptions.QException.class,
               () -> firstService.start(first));
            assertSame(replacement, QSessionStoreQBitContext.getProvider(first));
            assertSame(replacement, QSessionStoreRegistry.getInstance(first).getProvider().orElseThrow());
         }
         finally
         {
            firstService.afterApplicationStop();
            secondService.afterApplicationStop();
            firstPool.afterApplicationStop();
            secondPool.afterApplicationStop();
            for(var instance : List.of(first, second))
            {
               QSessionStoreQBitContext.clear(instance);
               QSessionStoreRegistry.getInstance(instance).clear();
            }
            QContext.setQInstance(original);
         }
      }
   }



   /***************************************************************************
    ** Exercise real launcher callbacks and rollback after a later service fails.
    ***************************************************************************/
   @Test
   void applicationLauncherOwnsNormalAndFailedShutdown() throws Exception
   {
      var original = QContext.capture();
      try(GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379))
      {
         redis.start();
         QInstance instance = host(redis, "launcher", true);
         var http = new java.util.concurrent.atomic.AtomicReference<org.eclipse.jetty.server.Server>();
         var launcher = com.kingsrook.qqq.middleware.javalin.QApplicationLauncher.run(
            application(instance), launcherConfig(http));
         var pool = RedisRuntimeService.connection(instance, "sessions");
         try
         {
            assertTrue(http.get().isStarted());
            assertTrue(launcher.getStartedServiceNames().containsAll(List.of("redis", "shared-redis-session-store")));
            var provider = QSessionStoreQBitContext.getProvider(instance);
            provider.store("launcher-session", new QSession().withValue("owner", "launcher"), null);
            assertEquals("launcher", provider.load("launcher-session").orElseThrow().getValue("owner"));
            QContext.clear();
         }
         finally
         {
            launcher.stop();
         }
         launcher.stop();
         assertTrue(http.get().isStopped());
         assertNull(QSessionStoreQBitContext.getProvider(instance));
         assertNull(QSessionStoreQBitContext.getConfig(instance));
         assertTrue(QSessionStoreRegistry.getInstance(instance).getProvider().isEmpty());
         assertThrows(IllegalStateException.class, () -> RedisRuntimeService.connection(instance, "sessions"));
         assertThrows(RuntimeException.class, () -> pool.client().ping());

         QInstance failing = host(redis, "failed-launcher", true);
         failing.withRuntimeService(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(FailAfterSessions.class));
         var failedHttp = new java.util.concurrent.atomic.AtomicReference<org.eclipse.jetty.server.Server>();
         var failure = assertThrows(com.kingsrook.qqq.backend.core.exceptions.QException.class,
            () -> com.kingsrook.qqq.middleware.javalin.QApplicationLauncher.run(application(failing), launcherConfig(failedHttp)));
         assertTrue(failure.getMessage().contains("owned late failure"));
         assertNotNull(failedHttp.get());
         assertTrue(failedHttp.get().isStopped());
         assertNull(QSessionStoreQBitContext.getProvider(failing));
         assertNull(QSessionStoreQBitContext.getConfig(failing));
         assertTrue(QSessionStoreRegistry.getInstance(failing).getProvider().isEmpty());
         assertThrows(IllegalStateException.class, () -> RedisRuntimeService.connection(failing, "sessions"));

         redis.execInContainer("redis-cli", "CONFIG", "SET", "requirepass", "owned-startup-password");
         QInstance early = host(redis, "early-failure", true);
         var configuredProvider = QSessionStoreQBitContext.getProvider(early);
         var earlyHttp = new java.util.concurrent.atomic.AtomicReference<org.eclipse.jetty.server.Server>();
         var earlyFailure = assertThrows(com.kingsrook.qqq.backend.core.exceptions.QException.class,
            () -> com.kingsrook.qqq.middleware.javalin.QApplicationLauncher.run(application(early), launcherConfig(earlyHttp)));
         assertTrue(earlyFailure.getMessage().contains("Redis startup failed"));
         assertTrue(earlyHttp.get().isStopped());
         assertThrows(IllegalStateException.class, () -> RedisRuntimeService.connection(early, "sessions"));
         assertSame(configuredProvider, QSessionStoreQBitContext.getProvider(early));
         assertThrows(IllegalStateException.class, () -> configuredProvider.load("unstarted"));
         assertEquals(0, redis.execInContainer("redis-cli", "-a", "owned-startup-password",
            "CONFIG", "SET", "requirepass", "").getExitCode());
         var retriedHttp = new java.util.concurrent.atomic.AtomicReference<org.eclipse.jetty.server.Server>();
         var retried = com.kingsrook.qqq.middleware.javalin.QApplicationLauncher.run(application(early), launcherConfig(retriedHttp));
         try
         {
            assertTrue(retriedHttp.get().isStarted());
            configuredProvider.store("retry", new QSession(), null);
            assertTrue(configuredProvider.load("retry").isPresent());
         }
         finally
         {
            retried.stop();
         }
         assertTrue(retriedHttp.get().isStopped());
         assertNull(QSessionStoreQBitContext.getProvider(early));
         assertTrue(QSessionStoreRegistry.getInstance(early).getProvider().isEmpty());
         assertThrows(IllegalStateException.class, () -> RedisRuntimeService.connection(early, "sessions"));
      }
      finally
      {
         QContext.clear();
         QContext.init(original);
      }
   }



   /***************************************************************************
    ** The launcher receives the same already-produced metadata as the runtime tests.
    ***************************************************************************/
   private com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication application(QInstance instance)
   {
      return new com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication()
      {
         /**********************************************************************
          ** Return this test's owned application, never a global fixture.
          **********************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      };
   }



   /***************************************************************************
    ** Bind an ephemeral loopback HTTP port and observe the real Jetty lifecycle.
    ***************************************************************************/
   private com.kingsrook.qqq.middleware.javalin.QApplicationLauncherConfig launcherConfig(
      java.util.concurrent.atomic.AtomicReference<org.eclipse.jetty.server.Server> http)
   {
      return new com.kingsrook.qqq.middleware.javalin.QApplicationLauncherConfig()
         .withRegisterShutdownHook(false).withServerCustomizer(server -> server.withPort(0)
            .withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
            .withJavalinConfigCustomizer(config ->
            {
               config.jetty.host = "127.0.0.1";
               config.jetty.modifyServer(http::set);
            }));
   }



   /***************************************************************************
    ** Fail only after proving Redis and session registration have started.
    ***************************************************************************/
   public static class FailAfterSessions implements com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface
   {
      /***********************************************************************
       ** Stable failure-stage name for launcher diagnostics.
       ***********************************************************************/
      @Override
      public String getName()
      {
         return "owned-late-failure";
      }



      /***********************************************************************
       ** The thrown message proves the preceding runtime dependencies were ready.
       ***********************************************************************/
      @Override
      public void start(QInstance instance) throws com.kingsrook.qqq.backend.core.exceptions.QException
      {
         if(QSessionStoreQBitContext.getProvider(instance) == null
            || !"PONG".equals(RedisRuntimeService.connection(instance, "sessions").client().ping()))
         {
            throw new com.kingsrook.qqq.backend.core.exceptions.QException("Preceding services were not ready");
         }
         throw new com.kingsrook.qqq.backend.core.exceptions.QException("owned late failure");
      }



      /***********************************************************************
       ** No resources are acquired by this deliberately failing service.
       ***********************************************************************/
      @Override
      public void stop()
      {
      }
   }



   /***************************************************************************
    ** Reject bad settings or missing infrastructure before publishing qbit metadata.
    ***************************************************************************/
   @Test
   void invalidConfigurationDoesNotRegister() throws Exception
   {
      assertThrows(IllegalArgumentException.class,
         () -> new SharedRedisSessionStoreSettings("", "test", "app", "auth", 100));
      assertThrows(IllegalArgumentException.class,
         () -> new SharedRedisSessionStoreSettings("pool", "test", "app", "auth", 0));
      assertThrows(IllegalArgumentException.class,
         () -> new SharedRedisSessionStoreSettings("pool", "test", "app", "auth", 1048577));
      QInstance instance = new QInstance();
      var config = new QSessionStoreQBitConfig().withProviderType(QSessionStoreProviderType.SHARED_REDIS);
      assertThrows(com.kingsrook.qqq.backend.core.exceptions.QException.class,
         () -> new QSessionStoreQBitProducer().withConfig(config).produce(instance, "session-store"));
      config.withSharedRedis(new SharedRedisSessionStoreSettings("missing", "test", "app", "auth", 65536));
      assertThrows(IllegalArgumentException.class,
         () -> new QSessionStoreQBitProducer().withConfig(config).produce(instance, "session-store"));
      assertTrue(instance.getQBits().isEmpty());
      assertTrue(QSessionStoreRegistry.getInstance(instance).getProvider().isEmpty());
      assertNull(QSessionStoreQBitContext.getProvider(instance));
      config.withDefaultTtl(Duration.ZERO);
      assertThrows(com.kingsrook.qqq.backend.core.exceptions.QException.class,
         () -> new QSessionStoreQBitProducer().withConfig(config).produce(instance, "session-store"));
   }



   /***************************************************************************
    ** Compose framework metadata and bind the session provider before pool startup.
    ***************************************************************************/
   private QInstance host(GenericContainer<?> redis, String application, boolean sliding) throws Exception
   {
      var instance = new QInstance();
      instance.addBackend(new com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData()
         .withName("memory").withBackendType(com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule.class));
      instance.withInstanceDefaultAuthentication(new com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData()
         .withName("test").withType(com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType.FULLY_ANONYMOUS));
      instance.addTable(new com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData().withName("record").withBackendName("memory").withPrimaryKeyField("id")
         .withField(new com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData("id", com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.INTEGER)));
      new RedisQBitProducer().withQBitConfig(new RedisQBitConfig().withConnection("sessions",
         new RedisConnectionConfig().withHost(redis.getHost()).withPort(redis.getMappedPort(6379))
            .withTls(false).withSocketTimeoutMillis(300).withConnectionTimeoutMillis(300)))
         .produce(instance);
      new QSessionStoreQBitProducer().withConfig(new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.SHARED_REDIS)
         .withSharedRedis(new SharedRedisSessionStoreSettings("sessions", "test", application, "authentication", 65536))
         .withDefaultTtl(Duration.ofSeconds(30)).withEnableSlidingExpiration(sliding))
         .produce(instance, "session-store");
      assertNull(instance.getProcess("cleanExpiredSessions"));
      return instance;
   }
}
