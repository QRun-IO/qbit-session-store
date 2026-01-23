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
import com.kingsrook.qbits.sessionstore.processes.CleanExpiredSessionsStep;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;


/*******************************************************************************
 ** Metadata producer for the CleanExpiredSessions process.
 *******************************************************************************/
public class CleanExpiredSessionsProcessMetaDataProducer
{
   public static final String PROCESS_NAME = "cleanExpiredSessions";



   /***************************************************************************
    ** Produce the cleanup process metadata into the QInstance.
    ***************************************************************************/
   public static void produce(QInstance qInstance, QSessionStoreQBitConfig config)
   {
      String processName = config.applyPrefix(PROCESS_NAME);

      QProcessMetaData process = new QProcessMetaData()
         .withName(processName)
         .withLabel("Clean Expired Sessions")
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(CleanExpiredSessionsStep.class))
         ));

      if(Boolean.TRUE.equals(config.getEnableCleanupProcess()))
      {
         Integer intervalSeconds = config.getCleanupIntervalSeconds();
         if(intervalSeconds == null || intervalSeconds <= 0)
         {
            intervalSeconds = 300; // Default to 5 minutes
         }

         process.withSchedule(new QScheduleMetaData()
            .withRepeatSeconds(intervalSeconds)
            .withDescription("Clean expired sessions - runs every " + intervalSeconds + " seconds"));
      }

      qInstance.addProcess(process);
   }

}
