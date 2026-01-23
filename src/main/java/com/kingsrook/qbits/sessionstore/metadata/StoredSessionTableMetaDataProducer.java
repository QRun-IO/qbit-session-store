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


import java.util.List;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qbits.sessionstore.model.StoredSession;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Metadata producer for the StoredSession table.
 *******************************************************************************/
public class StoredSessionTableMetaDataProducer
{

   /***************************************************************************
    ** Produce the StoredSession table metadata into the QInstance.
    ***************************************************************************/
   public static void produce(QInstance qInstance, QSessionStoreQBitConfig config)
   {
      String tableName = config.applyPrefix(config.getTableName());

      QTableMetaData table = new QTableMetaData()
         .withName(tableName)
         .withLabel("Stored Session")
         .withBackendName(config.getBackendName())
         .withPrimaryKeyField(StoredSession.FIELD_ID)
         .withRecordLabelFormat("%s")
         .withRecordLabelFields(List.of(StoredSession.FIELD_SESSION_UUID))
         .withUniqueKey(new UniqueKey(StoredSession.FIELD_SESSION_UUID))
         .withField(new QFieldMetaData(StoredSession.FIELD_ID, QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData(StoredSession.FIELD_SESSION_UUID, QFieldType.STRING)
            .withLabel("Session UUID")
            .withIsRequired(true)
            .withIsEditable(false)
            .withMaxLength(36))
         .withField(new QFieldMetaData(StoredSession.FIELD_USER_ID, QFieldType.STRING)
            .withLabel("User ID")
            .withIsEditable(false))
         .withField(new QFieldMetaData(StoredSession.FIELD_SESSION_DATA, QFieldType.TEXT)
            .withLabel("Session Data (JSON)")
            .withIsEditable(false))
         .withField(new QFieldMetaData(StoredSession.FIELD_EXPIRES_AT, QFieldType.DATE_TIME)
            .withLabel("Expires At")
            .withIsRequired(true))
         .withField(new QFieldMetaData(StoredSession.FIELD_CREATE_DATE, QFieldType.DATE_TIME)
            .withLabel("Create Date")
            .withIsEditable(false))
         .withField(new QFieldMetaData(StoredSession.FIELD_MODIFY_DATE, QFieldType.DATE_TIME)
            .withLabel("Modify Date")
            .withIsEditable(false));

      qInstance.addTable(table);
   }

}
