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
import com.kingsrook.qbits.sessionstore.model.StoredSession;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for StoredSessionTableMetaDataProducer.
 *******************************************************************************/
class StoredSessionTableMetaDataProducerTest
{

   /***************************************************************************
    ** Test producing table metadata with default settings.
    ***************************************************************************/
   @Test
   void testProduce_defaultSettings()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend");

      StoredSessionTableMetaDataProducer.produce(qInstance, config);

      QTableMetaData table = qInstance.getTable("storedSession");
      assertThat(table).isNotNull();
      assertThat(table.getName()).isEqualTo("storedSession");
      assertThat(table.getLabel()).isEqualTo("Stored Session");
      assertThat(table.getBackendName()).isEqualTo("testBackend");
      assertThat(table.getPrimaryKeyField()).isEqualTo(StoredSession.FIELD_ID);
   }



   /***************************************************************************
    ** Test producing table metadata with prefix.
    ***************************************************************************/
   @Test
   void testProduce_withPrefix()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withTableNamePrefix("myapp_");

      StoredSessionTableMetaDataProducer.produce(qInstance, config);

      QTableMetaData table = qInstance.getTable("myapp_storedSession");
      assertThat(table).isNotNull();
      assertThat(table.getName()).isEqualTo("myapp_storedSession");
   }



   /***************************************************************************
    ** Test producing table metadata with custom table name.
    ***************************************************************************/
   @Test
   void testProduce_customTableName()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend")
         .withTableName("customSessionTable");

      StoredSessionTableMetaDataProducer.produce(qInstance, config);

      QTableMetaData table = qInstance.getTable("customSessionTable");
      assertThat(table).isNotNull();
   }



   /***************************************************************************
    ** Test that all expected fields are present.
    ***************************************************************************/
   @Test
   void testProduce_allFieldsPresent()
   {
      QInstance qInstance = new QInstance();
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend");

      StoredSessionTableMetaDataProducer.produce(qInstance, config);

      QTableMetaData table = qInstance.getTable("storedSession");
      assertThat(table.getField(StoredSession.FIELD_ID)).isNotNull();
      assertThat(table.getField(StoredSession.FIELD_SESSION_UUID)).isNotNull();
      assertThat(table.getField(StoredSession.FIELD_USER_ID)).isNotNull();
      assertThat(table.getField(StoredSession.FIELD_SESSION_DATA)).isNotNull();
      assertThat(table.getField(StoredSession.FIELD_EXPIRES_AT)).isNotNull();
      assertThat(table.getField(StoredSession.FIELD_CREATE_DATE)).isNotNull();
      assertThat(table.getField(StoredSession.FIELD_MODIFY_DATE)).isNotNull();
   }

}
