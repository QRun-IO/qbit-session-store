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


/*******************************************************************************
 ** Strategy interface for session storage providers in the QBit.
 **
 ** Extends the core QSessionStoreProviderInterface and adds QBit-specific
 ** methods for configuration, cleanup, and monitoring.
 **
 ** Implementations provide different backends for session persistence:
 ** - InMemory: ConcurrentHashMap for dev/testing
 ** - TableBased: QQQ table storage for multi-instance persistence
 ** - Redis: Distributed caching for HA deployments
 ** - Custom: User-provided via QCodeReference
 *******************************************************************************/
public interface QSessionStoreProviderInterface
   extends com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreProviderInterface
{

   /***************************************************************************
    ** Clean up expired sessions.
    ***************************************************************************/
   void cleanExpired();


   /***************************************************************************
    ** Get provider status for monitoring.
    ***************************************************************************/
   String status();


   /***************************************************************************
    ** Configure the provider with QBit config. Called after construction.
    ***************************************************************************/
   default void configure(QSessionStoreQBitConfig config)
   {
      // Default no-op; providers can override to receive configuration
   }

}
