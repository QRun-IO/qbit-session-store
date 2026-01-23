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


import com.kingsrook.qbits.sessionstore.QSessionStoreProviderInterface;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitContext;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;


/*******************************************************************************
 ** Backend step that cleans up expired sessions.
 *******************************************************************************/
public class CleanExpiredSessionsStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(CleanExpiredSessionsStep.class);



   /***************************************************************************
    ** Execute the cleanup step.
    ***************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      QSessionStoreProviderInterface provider = QSessionStoreQBitContext.getProvider();
      if(provider == null)
      {
         LOG.warn("No session store provider configured - skipping cleanup");
         return;
      }

      LOG.debug("Running session cleanup");
      provider.cleanExpired();
      LOG.debug("Session cleanup complete");
   }

}
