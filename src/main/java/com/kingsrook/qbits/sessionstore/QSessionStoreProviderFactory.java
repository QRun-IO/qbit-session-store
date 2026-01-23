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


import com.kingsrook.qbits.sessionstore.providers.InMemorySessionStoreProvider;
import com.kingsrook.qbits.sessionstore.providers.RedisSessionStoreProvider;
import com.kingsrook.qbits.sessionstore.providers.TableBasedSessionStoreProvider;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Factory for creating session store providers based on configuration.
 *******************************************************************************/
public class QSessionStoreProviderFactory
{

   /***************************************************************************
    ** Create a provider instance based on the configured type.
    ***************************************************************************/
   public static QSessionStoreProviderInterface createProvider(QSessionStoreQBitConfig config) throws QException
   {
      QSessionStoreProviderInterface provider = switch(config.getProviderType())
      {
         case IN_MEMORY -> new InMemorySessionStoreProvider();
         case TABLE_BASED -> new TableBasedSessionStoreProvider();
         case REDIS -> new RedisSessionStoreProvider();
         case CUSTOM -> QCodeLoader.getAdHoc(QSessionStoreProviderInterface.class, config.getCustomProviderCodeReference());
      };

      provider.configure(config);
      return provider;
   }

}
