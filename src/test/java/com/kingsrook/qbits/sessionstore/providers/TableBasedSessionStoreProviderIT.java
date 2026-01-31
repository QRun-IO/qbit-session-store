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


import java.sql.Connection;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderType;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qbits.sessionstore.metadata.StoredSessionTableMetaDataProducer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Integration tests for TableBasedSessionStoreProvider using H2 database.
 *******************************************************************************/
class TableBasedSessionStoreProviderIT
{
   private static final String BACKEND_NAME = "h2Backend";
   private static final String TABLE_NAME   = "storedSession";

   private TableBasedSessionStoreProvider provider;
   private QInstance                       qInstance;
   private RDBMSBackendMetaData            backend;



   @BeforeEach
   void setUp() throws Exception
   {
      /////////////////////////////////////////////////////////////////////////
      // Create QInstance with H2 backend                                    //
      /////////////////////////////////////////////////////////////////////////
      qInstance = new QInstance();

      backend = new RDBMSBackendMetaData()
         .withName(BACKEND_NAME)
         .withVendor("h2")
         .withHostName("mem")
         .withDatabaseName("sessionstoretest")
         .withUsername("sa");

      /////////////////////////////////////////////////////////////////////////
      // Get auto-generated JDBC URL and add H2-specific settings            //
      // DATABASE_TO_UPPER=FALSE prevents H2 from uppercasing names          //
      /////////////////////////////////////////////////////////////////////////
      String jdbcUrl = ConnectionManager.getJdbcUrl(backend);
      jdbcUrl += ";DATABASE_TO_UPPER=FALSE";
      backend.setJdbcUrl(jdbcUrl);

      qInstance.addBackend(backend);

      /////////////////////////////////////////////////////////////////////////
      // Add authentication (required for QInstance validation)               //
      /////////////////////////////////////////////////////////////////////////
      qInstance.setAuthentication(new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.FULLY_ANONYMOUS));

      /////////////////////////////////////////////////////////////////////////
      // Create config and produce table metadata                            //
      /////////////////////////////////////////////////////////////////////////
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName(BACKEND_NAME)
         .withTableName(TABLE_NAME)
         .withDefaultTtl(Duration.ofMinutes(30));

      StoredSessionTableMetaDataProducer.produce(qInstance, config);

      /////////////////////////////////////////////////////////////////////////
      // Initialize QContext                                                 //
      /////////////////////////////////////////////////////////////////////////
      QContext.init(qInstance, new QSession());

      /////////////////////////////////////////////////////////////////////////
      // Create the database table                                           //
      // Note: RDBMS module uses camelCase names directly with backticks     //
      // Use TIMESTAMP WITH TIME ZONE to preserve Instant timezone info      //
      /////////////////////////////////////////////////////////////////////////
      try(Connection conn = ConnectionManager.getConnection(backend))
      {
         conn.createStatement().execute("DROP TABLE IF EXISTS `storedSession`");
         conn.createStatement().execute("""
            CREATE TABLE `storedSession` (
               `id` INTEGER AUTO_INCREMENT PRIMARY KEY,
               `sessionUuid` VARCHAR(36) NOT NULL UNIQUE,
               `userId` VARCHAR(255),
               `sessionData` TEXT,
               `expiresAt` TIMESTAMP WITH TIME ZONE NOT NULL,
               `createDate` TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
               `modifyDate` TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            )
            """);
      }

      /////////////////////////////////////////////////////////////////////////
      // Configure provider                                                  //
      /////////////////////////////////////////////////////////////////////////
      provider = new TableBasedSessionStoreProvider();
      provider.configure(config);
   }



   @AfterEach
   void tearDown() throws Exception
   {
      /////////////////////////////////////////////////////////////////////////
      // Clean up database and context                                       //
      /////////////////////////////////////////////////////////////////////////
      try(Connection conn = ConnectionManager.getConnection(backend))
      {
         conn.createStatement().execute("DROP TABLE IF EXISTS `storedSession`");
      }
      catch(Exception e)
      {
         // Ignore cleanup errors
      }
      QContext.clear();
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
    ** Test expired session is not returned.
    ***************************************************************************/
   @Test
   void testExpiredSession() throws InterruptedException
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "expireUser");

      // Store with very short TTL
      provider.store(sessionUuid, session, Duration.ofMillis(100));

      // Wait for expiration
      Thread.sleep(200);

      // Session should be detected as expired and removed on load
      assertThat(provider.load(sessionUuid)).isEmpty();
   }



   /***************************************************************************
    ** Test touch resets TTL.
    ***************************************************************************/
   @Test
   void testTouch()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "touchUser");

      provider.store(sessionUuid, session, Duration.ofMinutes(1));
      provider.touch(sessionUuid);

      // Session should still be loadable
      Optional<QSession> loaded = provider.load(sessionUuid);
      assertThat(loaded).isPresent();
   }



   /***************************************************************************
    ** Test loadAndTouch returns session and updates expiresAt.
    ***************************************************************************/
   @Test
   void testLoadAndTouch()
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "loadAndTouchUser");

      provider.store(sessionUuid, session, Duration.ofMinutes(1));

      Optional<QSession> loaded = provider.loadAndTouch(sessionUuid);
      assertThat(loaded).isPresent();
      assertThat(loaded.get().getUuid()).isEqualTo(sessionUuid);
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
    ** Test loadAndTouch removes expired session.
    ***************************************************************************/
   @Test
   void testLoadAndTouch_expired() throws InterruptedException
   {
      String sessionUuid = UUID.randomUUID().toString();
      QSession session = createTestSession(sessionUuid, "expiredLoadAndTouch");

      // Store with very short TTL
      provider.store(sessionUuid, session, Duration.ofMillis(100));

      // Wait for expiration
      Thread.sleep(200);

      // loadAndTouch should detect expiration and remove
      Optional<QSession> loaded = provider.loadAndTouch(sessionUuid);
      assertThat(loaded).isEmpty();

      // Subsequent load should also return empty
      assertThat(provider.load(sessionUuid)).isEmpty();
   }



   /***************************************************************************
    ** Test cleanExpired removes only expired sessions.
    ***************************************************************************/
   @Test
   void testCleanExpired() throws InterruptedException
   {
      String uuid1 = UUID.randomUUID().toString();
      String uuid2 = UUID.randomUUID().toString();

      // Store one session with short TTL, one with long TTL
      provider.store(uuid1, createTestSession(uuid1, "expireUser"), Duration.ofMillis(100));
      provider.store(uuid2, createTestSession(uuid2, "persistUser"), Duration.ofHours(1));

      // Wait for first session to expire
      Thread.sleep(200);

      // Clean expired sessions
      provider.cleanExpired();

      // First session should be removed, second should remain
      assertThat(provider.load(uuid1)).isEmpty();
      assertThat(provider.load(uuid2)).isPresent();
   }



   /***************************************************************************
    ** Test status returns session count.
    ***************************************************************************/
   @Test
   void testStatus()
   {
      String uuid1 = UUID.randomUUID().toString();
      String uuid2 = UUID.randomUUID().toString();

      provider.store(uuid1, createTestSession(uuid1, "user1"), null);
      provider.store(uuid2, createTestSession(uuid2, "user2"), null);

      String status = provider.status();

      assertThat(status).contains("TableBased");
      assertThat(status).contains("2 sessions");
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
    ** Test getDefaultTtl returns configured value.
    ***************************************************************************/
   @Test
   void testGetDefaultTtl()
   {
      assertThat(provider.getDefaultTtl()).isEqualTo(Duration.ofMinutes(30));
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
