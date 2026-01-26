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
import com.kingsrook.qqq.backend.core.exceptions.QException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for QSessionStoreProviderFactory.
 *******************************************************************************/
class QSessionStoreProviderFactoryTest
{

   /***************************************************************************
    ** Test creating IN_MEMORY provider.
    ***************************************************************************/
   @Test
   void testCreateProvider_inMemory() throws QException
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY);

      QSessionStoreProviderInterface provider = QSessionStoreProviderFactory.createProvider(config);

      assertThat(provider).isInstanceOf(InMemorySessionStoreProvider.class);
   }



   /***************************************************************************
    ** Test creating TABLE_BASED provider.
    ***************************************************************************/
   @Test
   void testCreateProvider_tableBased() throws QException
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("testBackend");

      QSessionStoreProviderInterface provider = QSessionStoreProviderFactory.createProvider(config);

      assertThat(provider).isInstanceOf(TableBasedSessionStoreProvider.class);
   }



   /***************************************************************************
    ** Test creating REDIS provider.
    ***************************************************************************/
   @Test
   void testCreateProvider_redis() throws QException
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("localhost");

      QSessionStoreProviderInterface provider = QSessionStoreProviderFactory.createProvider(config);

      assertThat(provider).isInstanceOf(RedisSessionStoreProvider.class);
   }

}
