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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.sessionstore.metadata.CleanExpiredSessionsProcessMetaDataProducer;
import com.kingsrook.qbits.sessionstore.metadata.StoredSessionTableMetaDataProducer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Producer for the Session Store QBit.
 *******************************************************************************/
public class QSessionStoreQBitProducer implements QBitProducer
{
   private static final QLogger LOG = QLogger.getLogger(QSessionStoreQBitProducer.class);

   public static final String GROUP_ID    = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "qbit-session-store";
   public static final String VERSION     = "0.1.0";

   private QSessionStoreQBitConfig config;



   /***************************************************************************
    ** Produce this QBit into the given QInstance.
    ***************************************************************************/
   @Override
   public void produce(QInstance qInstance, String namespace) throws QException
   {
      /////////////////////////////
      // Validate configuration //
      /////////////////////////////
      List<String> errors = new ArrayList<>();
      config.validate(qInstance, errors);
      if(!errors.isEmpty())
      {
         throw new QException("SessionStore QBit configuration errors: " + String.join(", ", errors));
      }

      ///////////////////////////////
      // Register QBit identity //
      ///////////////////////////////
      QBitMetaData qBitMetaData = new QBitMetaData()
         .withGroupId(GROUP_ID)
         .withArtifactId(ARTIFACT_ID)
         .withVersion(VERSION)
         .withNamespace(namespace)
         .withConfig(config);

      qInstance.addQBit(qBitMetaData);

      ///////////////////////////////
      // Create provider instance //
      ///////////////////////////////
      QSessionStoreProviderInterface provider = QSessionStoreProviderFactory.createProvider(config);
      LOG.info("Created session store provider", logPair("type", config.getProviderType()));

      ///////////////////////////////
      // Produce metadata for TABLE_BASED provider //
      ///////////////////////////////
      if(config.getProviderType() == QSessionStoreProviderType.TABLE_BASED)
      {
         StoredSessionTableMetaDataProducer.produce(qInstance, config);
         LOG.info("Produced StoredSession table", logPair("tableName", config.applyPrefix(config.getTableName())));
      }

      ///////////////////////////////
      // Produce cleanup process //
      ///////////////////////////////
      if(Boolean.TRUE.equals(config.getEnableCleanupProcess()))
      {
         CleanExpiredSessionsProcessMetaDataProducer.produce(qInstance, config);
         LOG.info("Produced cleanup process");
      }

      ///////////////////////////////
      // Store in context //
      ///////////////////////////////
      QSessionStoreQBitContext.setConfig(config);
      QSessionStoreQBitContext.setProvider(provider);

      ///////////////////////////////
      // Register with core registry //
      ///////////////////////////////
      QSessionStoreRegistry.getInstance().register(provider);
      LOG.info("Registered session store provider with core registry");
   }



   /***************************************************************************
    ** Fluent setter for config.
    ***************************************************************************/
   public QSessionStoreQBitProducer withConfig(QSessionStoreQBitConfig config)
   {
      this.config = config;
      return this;
   }



   /***************************************************************************
    ** Getter for config.
    ***************************************************************************/
   public QSessionStoreQBitConfig getConfig()
   {
      return config;
   }

}
