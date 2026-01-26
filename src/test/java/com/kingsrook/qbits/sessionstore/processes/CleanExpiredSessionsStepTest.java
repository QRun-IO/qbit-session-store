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
package com.kingsrook.qbits.sessionstore.processes;


import java.time.Duration;
import java.util.UUID;
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderType;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitContext;
import com.kingsrook.qbits.sessionstore.providers.InMemorySessionStoreProvider;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for CleanExpiredSessionsStep.
 *******************************************************************************/
class CleanExpiredSessionsStepTest
{
   private CleanExpiredSessionsStep step;
   private InMemorySessionStoreProvider provider;



   @BeforeEach
   void setUp()
   {
      step = new CleanExpiredSessionsStep();
      provider = new InMemorySessionStoreProvider();
      provider.configure(new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withDefaultTtl(Duration.ofMinutes(30))
         .withMaxCacheSize(100));
      QSessionStoreQBitContext.setProvider(provider);
   }



   @AfterEach
   void tearDown()
   {
      QSessionStoreQBitContext.setProvider(null);
      QSessionStoreQBitContext.setConfig(null);
   }



   /***************************************************************************
    ** Test running cleanup with no provider configured.
    ***************************************************************************/
   @Test
   void testRun_noProvider() throws QException
   {
      QSessionStoreQBitContext.setProvider(null);

      RunBackendStepInput input = new RunBackendStepInput();
      RunBackendStepOutput output = new RunBackendStepOutput();

      // Should not throw - just logs warning and returns
      step.run(input, output);
   }



   /***************************************************************************
    ** Test running cleanup with provider configured.
    ***************************************************************************/
   @Test
   void testRun_withProvider() throws QException
   {
      // Add some sessions
      String uuid1 = UUID.randomUUID().toString();
      String uuid2 = UUID.randomUUID().toString();
      provider.store(uuid1, createTestSession(uuid1), Duration.ofMillis(1));
      provider.store(uuid2, createTestSession(uuid2), Duration.ofHours(1));

      // Wait for first to expire
      try { Thread.sleep(50); } catch (InterruptedException e) { }

      RunBackendStepInput input = new RunBackendStepInput();
      RunBackendStepOutput output = new RunBackendStepOutput();

      step.run(input, output);

      // First session should be cleaned, second should remain
      assertThat(provider.load(uuid1)).isEmpty();
      assertThat(provider.load(uuid2)).isPresent();
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
