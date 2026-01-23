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
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for InMemorySessionStoreProvider.
 *******************************************************************************/
class InMemorySessionStoreProviderTest
{
   private InMemorySessionStoreProvider provider;



   @BeforeEach
   void setUp()
   {
      provider = new InMemorySessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withDefaultTtl(Duration.ofMinutes(30))
         .withMaxCacheSize(100);

      provider.configure(config);
   }



   /***************************************************************************
    ** Test store and load.
    ***************************************************************************/
   @Test
   void testStoreAndLoad()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid);

      provider.store(sessionUuid, session, null);
      Optional<QSession> loaded = provider.load(sessionUuid);

      assertThat(loaded).isPresent();
      assertThat(loaded.get().getUuid()).isEqualTo(sessionUuid);
   }



   /***************************************************************************
    ** Test load nonexistent session.
    ***************************************************************************/
   @Test
   void testLoad_notFound()
   {
      Optional<QSession> loaded = provider.load("nonexistent");

      assertThat(loaded).isEmpty();
   }



   /***************************************************************************
    ** Test remove session.
    ***************************************************************************/
   @Test
   void testRemove()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid);

      provider.store(sessionUuid, session, null);
      assertThat(provider.load(sessionUuid)).isPresent();

      provider.remove(sessionUuid);
      assertThat(provider.load(sessionUuid)).isEmpty();
   }



   /***************************************************************************
    ** Test expired session is not returned.
    ***************************************************************************/
   @Test
   void testExpiredSession() throws InterruptedException
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid);

      // Store with very short TTL
      provider.store(sessionUuid, session, Duration.ofMillis(50));

      // Wait for expiration
      Thread.sleep(100);

      Optional<QSession> loaded = provider.load(sessionUuid);
      assertThat(loaded).isEmpty();
   }



   /***************************************************************************
    ** Test touch resets TTL.
    ***************************************************************************/
   @Test
   void testTouch()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid);

      provider.store(sessionUuid, session, Duration.ofMinutes(1));
      provider.touch(sessionUuid);

      // Session should still be loadable
      Optional<QSession> loaded = provider.load(sessionUuid);
      assertThat(loaded).isPresent();
   }



   /***************************************************************************
    ** Test cleanExpired removes expired sessions.
    ***************************************************************************/
   @Test
   void testCleanExpired() throws InterruptedException
   {
      String sessionUuid1 = UUID.randomUUID().toString();
      String sessionUuid2 = UUID.randomUUID().toString();

      provider.store(sessionUuid1, createTestSession(sessionUuid1), Duration.ofMillis(50));
      provider.store(sessionUuid2, createTestSession(sessionUuid2), Duration.ofHours(1));

      // Wait for first session to expire
      Thread.sleep(100);

      provider.cleanExpired();

      assertThat(provider.load(sessionUuid1)).isEmpty();
      assertThat(provider.load(sessionUuid2)).isPresent();
   }



   /***************************************************************************
    ** Test status returns session count.
    ***************************************************************************/
   @Test
   void testStatus()
   {
      provider.store(UUID.randomUUID().toString(), createTestSession("1"), null);
      provider.store(UUID.randomUUID().toString(), createTestSession("2"), null);

      String status = provider.status();

      assertThat(status).contains("InMemory");
      assertThat(status).contains("2 sessions");
   }



   /***************************************************************************
    ** Helper to create a test session.
    ***************************************************************************/
   private QSession createTestSession(String uuid)
   {
      QSession session = new QSession();
      session.setUuid(uuid);
      session.setUser(new QUser().withIdReference("testUser"));
      return session;
   }

}
