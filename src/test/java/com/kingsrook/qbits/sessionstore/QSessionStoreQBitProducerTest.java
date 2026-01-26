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
package com.kingsrook.qbits.sessionstore;


import java.time.Duration;
import com.kingsrook.qbits.sessionstore.providers.InMemorySessionStoreProvider;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Tests for QSessionStoreQBitProducer.
 *******************************************************************************/
class QSessionStoreQBitProducerTest
{

   @AfterEach
   void tearDown()
   {
      QSessionStoreQBitContext.setConfig(null);
      QSessionStoreQBitContext.setProvider(null);
      // Note: Can't unregister from QSessionStoreRegistry - it doesn't accept null
   }



   /***************************************************************************
    ** Test producing IN_MEMORY provider.
    ***************************************************************************/
   @Test
   void testProduce_inMemory() throws QException
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withDefaultTtl(Duration.ofHours(2))
         .withEnableCleanupProcess(true);

      new QSessionStoreQBitProducer()
         .withConfig(config)
         .produce(qInstance, "testNamespace");

      // Verify QBit was registered
      assertThat(qInstance.getQBits()).hasSize(1);

      // Verify provider was created and stored in context
      assertThat(QSessionStoreQBitContext.getProvider()).isInstanceOf(InMemorySessionStoreProvider.class);
      assertThat(QSessionStoreQBitContext.getConfig()).isSameAs(config);

      // Verify cleanup process was created
      assertThat(qInstance.getProcess("cleanExpiredSessions")).isNotNull();

      // Verify registered with core registry
      assertThat(QSessionStoreRegistry.getInstance().getProvider()).isPresent();
   }



   /***************************************************************************
    ** Test producing with missing provider type fails validation.
    ***************************************************************************/
   @Test
   void testProduce_missingProviderType_throws()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig();

      QSessionStoreQBitProducer producer = new QSessionStoreQBitProducer()
         .withConfig(config);

      assertThatThrownBy(() -> producer.produce(qInstance, "test"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("providerType is required");
   }



   /***************************************************************************
    ** Test producing without cleanup process.
    ***************************************************************************/
   @Test
   void testProduce_cleanupDisabled() throws QException
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withEnableCleanupProcess(false);

      new QSessionStoreQBitProducer()
         .withConfig(config)
         .produce(qInstance, "test");

      // Process should not have a schedule
      assertThat(qInstance.getProcess("cleanExpiredSessions")).isNull();
   }



   /***************************************************************************
    ** Test fluent config getter/setter.
    ***************************************************************************/
   @Test
   void testConfigGetterSetter()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY);

      QSessionStoreQBitProducer producer = new QSessionStoreQBitProducer()
         .withConfig(config);

      assertThat(producer.getConfig()).isSameAs(config);
   }



   /***************************************************************************
    ** Test constants.
    ***************************************************************************/
   @Test
   void testConstants()
   {
      assertThat(QSessionStoreQBitProducer.GROUP_ID).isEqualTo("com.kingsrook.qbits");
      assertThat(QSessionStoreQBitProducer.ARTIFACT_ID).isEqualTo("qbit-session-store");
      assertThat(QSessionStoreQBitProducer.VERSION).isEqualTo("0.1.0");
   }

}
