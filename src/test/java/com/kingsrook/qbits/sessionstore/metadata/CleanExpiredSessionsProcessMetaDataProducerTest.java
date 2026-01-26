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
package com.kingsrook.qbits.sessionstore.metadata;


import com.kingsrook.qbits.sessionstore.QSessionStoreProviderType;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for CleanExpiredSessionsProcessMetaDataProducer.
 *******************************************************************************/
class CleanExpiredSessionsProcessMetaDataProducerTest
{

   /***************************************************************************
    ** Test producing process metadata with default settings.
    ***************************************************************************/
   @Test
   void testProduce_defaultSettings()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withEnableCleanupProcess(true);

      CleanExpiredSessionsProcessMetaDataProducer.produce(qInstance, config);

      QProcessMetaData process = qInstance.getProcess("cleanExpiredSessions");
      assertThat(process).isNotNull();
      assertThat(process.getName()).isEqualTo("cleanExpiredSessions");
      assertThat(process.getLabel()).isEqualTo("Clean Expired Sessions");
      assertThat(process.getSchedule()).isNotNull();
      assertThat(process.getSchedule().getRepeatSeconds()).isEqualTo(300);
   }



   /***************************************************************************
    ** Test producing process metadata with prefix.
    ***************************************************************************/
   @Test
   void testProduce_withPrefix()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withTableNamePrefix("myapp_")
         .withEnableCleanupProcess(true);

      CleanExpiredSessionsProcessMetaDataProducer.produce(qInstance, config);

      QProcessMetaData process = qInstance.getProcess("myapp_cleanExpiredSessions");
      assertThat(process).isNotNull();
   }



   /***************************************************************************
    ** Test producing process metadata with custom interval.
    ***************************************************************************/
   @Test
   void testProduce_customInterval()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withEnableCleanupProcess(true)
         .withCleanupIntervalSeconds(600);

      CleanExpiredSessionsProcessMetaDataProducer.produce(qInstance, config);

      QProcessMetaData process = qInstance.getProcess("cleanExpiredSessions");
      assertThat(process.getSchedule().getRepeatSeconds()).isEqualTo(600);
   }



   /***************************************************************************
    ** Test producing process metadata without schedule when cleanup disabled.
    ***************************************************************************/
   @Test
   void testProduce_cleanupDisabled()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withEnableCleanupProcess(false);

      CleanExpiredSessionsProcessMetaDataProducer.produce(qInstance, config);

      QProcessMetaData process = qInstance.getProcess("cleanExpiredSessions");
      assertThat(process).isNotNull();
      assertThat(process.getSchedule()).isNull();
   }



   /***************************************************************************
    ** Test process name constant.
    ***************************************************************************/
   @Test
   void testProcessNameConstant()
   {
      assertThat(CleanExpiredSessionsProcessMetaDataProducer.PROCESS_NAME).isEqualTo("cleanExpiredSessions");
   }

}
