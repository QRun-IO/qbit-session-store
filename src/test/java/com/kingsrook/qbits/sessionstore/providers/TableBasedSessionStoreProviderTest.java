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
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderType;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit tests for TableBasedSessionStoreProvider.
 ** Note: These tests verify configuration and error handling without a real database.
 ** Integration tests with H2 are in TableBasedSessionStoreProviderIT.
 *******************************************************************************/
class TableBasedSessionStoreProviderTest
{

   /***************************************************************************
    ** Test getDefaultTtl returns configured value.
    ***************************************************************************/
   @Test
   void testGetDefaultTtl()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withTableName("testTable")
         .withDefaultTtl(Duration.ofHours(6));

      provider.configure(config);

      assertThat(provider.getDefaultTtl()).isEqualTo(Duration.ofHours(6));
   }



   /***************************************************************************
    ** Test configure with table prefix.
    ***************************************************************************/
   @Test
   void testConfigure_withTablePrefix()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withTableName("storedSession")
         .withTableNamePrefix("myapp_")
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      assertThat(provider.getDefaultTtl()).isEqualTo(Duration.ofHours(1));
   }



   /***************************************************************************
    ** Test load returns empty when no QContext (error handling).
    ***************************************************************************/
   @Test
   void testLoad_noContext_returnsEmpty()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should return empty (with logged warning) rather than throwing
      assertThat(provider.load("test-uuid")).isEmpty();
   }



   /***************************************************************************
    ** Test loadAndTouch returns empty when no QContext.
    ***************************************************************************/
   @Test
   void testLoadAndTouch_noContext_returnsEmpty()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should return empty (with logged warning) rather than throwing
      assertThat(provider.loadAndTouch("test-uuid")).isEmpty();
   }



   /***************************************************************************
    ** Test store handles errors gracefully when no QContext.
    ***************************************************************************/
   @Test
   void testStore_noContext_doesNotThrow()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should not throw - just logs warning
      QSession session = new QSession();
      session.setUuid("test-uuid");
      provider.store("test-uuid", session, null);
   }



   /***************************************************************************
    ** Test remove handles errors gracefully when no QContext.
    ***************************************************************************/
   @Test
   void testRemove_noContext_doesNotThrow()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should not throw - just logs warning
      provider.remove("test-uuid");
   }



   /***************************************************************************
    ** Test touch handles errors gracefully when no QContext.
    ***************************************************************************/
   @Test
   void testTouch_noContext_doesNotThrow()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should not throw - just logs warning
      provider.touch("test-uuid");
   }



   /***************************************************************************
    ** Test cleanExpired handles errors gracefully when no QContext.
    ***************************************************************************/
   @Test
   void testCleanExpired_noContext_doesNotThrow()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should not throw - just logs warning
      provider.cleanExpired();
   }



   /***************************************************************************
    ** Test status handles errors gracefully when no QContext.
    ***************************************************************************/
   @Test
   void testStatus_noContext_returnsError()
   {
      TableBasedSessionStoreProvider provider = new TableBasedSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      String status = provider.status();
      assertThat(status).contains("TableBased");
      assertThat(status).contains("error");
   }

}
