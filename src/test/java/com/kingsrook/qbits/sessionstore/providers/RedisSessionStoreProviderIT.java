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
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderType;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/*******************************************************************************
 ** Integration tests for RedisSessionStoreProvider using Testcontainers.
 ** These tests require Docker to be running. If Docker is not available,
 ** the tests will be skipped.
 *******************************************************************************/
class RedisSessionStoreProviderIT
{
   private static final int REDIS_PORT = 6379;

   private static boolean                dockerAvailable = false;
   private static GenericContainer<?>    redis;
   private        RedisSessionStoreProvider provider;



   /***************************************************************************
    ** Check Docker availability and start container if available.
    ***************************************************************************/
   @BeforeAll
   static void startContainer()
   {
      try
      {
         dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
      }
      catch(Exception e)
      {
         dockerAvailable = false;
      }

      if(dockerAvailable)
      {
         try
         {
            redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
               .withExposedPorts(REDIS_PORT);
            redis.start();
         }
         catch(Exception e)
         {
            dockerAvailable = false;
            redis = null;
         }
      }
   }



   /***************************************************************************
    ** Stop container after all tests.
    ***************************************************************************/
   @AfterAll
   static void stopContainer()
   {
      if(redis != null)
      {
         redis.stop();
      }
   }



   @BeforeEach
   void setUp()
   {
      assumeTrue(dockerAvailable && redis != null && redis.isRunning(),
         "Docker is not available or Redis container is not running - skipping test");

      provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost(redis.getHost())
         .withRedisPort(redis.getMappedPort(REDIS_PORT))
         .withRedisKeyPrefix("test:session:")
         .withDefaultTtl(Duration.ofMinutes(30));

      provider.configure(config);
   }



   /***************************************************************************
    ** Test store and load.
    ***************************************************************************/
   @Test
   void testStoreAndLoad()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "testUser");

      provider.store(sessionUuid, session, null);
      Optional<QSession> loaded = provider.load(sessionUuid);

      assertThat(loaded).isPresent();
      assertThat(loaded.get().getUuid()).isEqualTo(sessionUuid);
      assertThat(loaded.get().getUser().getIdReference()).isEqualTo("testUser");
   }



   /***************************************************************************
    ** Test load nonexistent session returns empty.
    ***************************************************************************/
   @Test
   void testLoad_notFound()
   {
      Optional<QSession> loaded = provider.load("nonexistent-uuid");

      assertThat(loaded).isEmpty();
   }



   /***************************************************************************
    ** Test remove session.
    ***************************************************************************/
   @Test
   void testRemove()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "removeUser");

      provider.store(sessionUuid, session, null);
      assertThat(provider.load(sessionUuid)).isPresent();

      provider.remove(sessionUuid);
      assertThat(provider.load(sessionUuid)).isEmpty();
   }



   /***************************************************************************
    ** Test expired session is not returned (Redis native TTL).
    ***************************************************************************/
   @Test
   void testExpiredSession() throws InterruptedException
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "expireUser");

      // Store with 1 second TTL
      provider.store(sessionUuid, session, Duration.ofSeconds(1));

      // Verify session exists
      assertThat(provider.load(sessionUuid)).isPresent();

      // Wait for expiration
      Thread.sleep(1500);

      // Session should be gone
      assertThat(provider.load(sessionUuid)).isEmpty();
   }



   /***************************************************************************
    ** Test touch resets TTL.
    ***************************************************************************/
   @Test
   void testTouch() throws InterruptedException
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "touchUser");

      // Store with 2 second TTL
      provider.store(sessionUuid, session, Duration.ofSeconds(2));

      // Wait 1 second
      Thread.sleep(1000);

      // Touch to reset TTL
      provider.touch(sessionUuid);

      // Wait another 1.5 seconds (would have expired if not touched)
      Thread.sleep(1500);

      // Session should still exist
      assertThat(provider.load(sessionUuid)).isPresent();
   }



   /***************************************************************************
    ** Test loadAndTouch returns session and resets TTL.
    ***************************************************************************/
   @Test
   void testLoadAndTouch() throws InterruptedException
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "loadAndTouchUser");

      // Store with 2 second TTL
      provider.store(sessionUuid, session, Duration.ofSeconds(2));

      // Wait 1 second
      Thread.sleep(1000);

      // Load and touch (resets TTL)
      Optional<QSession> loaded = provider.loadAndTouch(sessionUuid);
      assertThat(loaded).isPresent();
      assertThat(loaded.get().getUuid()).isEqualTo(sessionUuid);

      // Wait another 1.5 seconds (would have expired if not touched)
      Thread.sleep(1500);

      // Session should still exist due to TTL reset
      assertThat(provider.load(sessionUuid)).isPresent();
   }



   /***************************************************************************
    ** Test loadAndTouch returns empty for nonexistent session.
    ***************************************************************************/
   @Test
   void testLoadAndTouch_notFound()
   {
      Optional<QSession> loaded = provider.loadAndTouch("nonexistent-uuid");

      assertThat(loaded).isEmpty();
   }



   /***************************************************************************
    ** Test cleanExpired is a no-op (Redis handles TTL natively).
    ***************************************************************************/
   @Test
   void testCleanExpired()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "cleanUser");

      provider.store(sessionUuid, session, null);

      // cleanExpired is a no-op for Redis, should not throw
      provider.cleanExpired();

      // Session should still exist
      assertThat(provider.load(sessionUuid)).isPresent();
   }



   /***************************************************************************
    ** Test status returns connected status.
    ***************************************************************************/
   @Test
   void testStatus()
   {
      String status = provider.status();

      assertThat(status).contains("Redis");
      assertThat(status).contains("connected");
   }



   /***************************************************************************
    ** Test store with custom TTL.
    ***************************************************************************/
   @Test
   void testStore_withCustomTtl() throws InterruptedException
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "customTtlUser");

      // Store with 1 second TTL (overriding default)
      provider.store(sessionUuid, session, Duration.ofSeconds(1));

      // Verify session exists
      assertThat(provider.load(sessionUuid)).isPresent();

      // Wait for expiration
      Thread.sleep(1500);

      // Session should be expired
      assertThat(provider.load(sessionUuid)).isEmpty();
   }



   /***************************************************************************
    ** Test multiple sessions.
    ***************************************************************************/
   @Test
   void testMultipleSessions()
   {
      String uuid1 = UUID.randomUUID().toString();
      String uuid2 = UUID.randomUUID().toString();
      String uuid3 = UUID.randomUUID().toString();

      provider.store(uuid1, createTestSession(uuid1, "user1"), null);
      provider.store(uuid2, createTestSession(uuid2, "user2"), null);
      provider.store(uuid3, createTestSession(uuid3, "user3"), null);

      assertThat(provider.load(uuid1)).isPresent();
      assertThat(provider.load(uuid2)).isPresent();
      assertThat(provider.load(uuid3)).isPresent();

      provider.remove(uuid2);

      assertThat(provider.load(uuid1)).isPresent();
      assertThat(provider.load(uuid2)).isEmpty();
      assertThat(provider.load(uuid3)).isPresent();
   }



   /***************************************************************************
    ** Test session data integrity (complex session object).
    ***************************************************************************/
   @Test
   void testSessionDataIntegrity()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "integrityUser");
      session.setValue("customKey", "customValue");

      provider.store(sessionUuid, session, null);
      Optional<QSession> loaded = provider.load(sessionUuid);

      assertThat(loaded).isPresent();
      assertThat(loaded.get().getUuid()).isEqualTo(sessionUuid);
      assertThat(loaded.get().getUser().getIdReference()).isEqualTo("integrityUser");
      assertThat(loaded.get().getValue("customKey")).isEqualTo("customValue");
   }



   /***************************************************************************
    ** Helper to create a test session.
    ***************************************************************************/
   private QSession createTestSession(String uuid, String userId)
   {
      QSession session = new QSession();
      session.setUuid(uuid);
      session.setUser(new QUser().withIdReference(userId));
      return session;
   }

}
