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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for QSessionStoreQBitContext.
 *******************************************************************************/
class QSessionStoreQBitContextTest
{

   @AfterEach
   void tearDown()
   {
      QSessionStoreQBitContext.setConfig(null);
      QSessionStoreQBitContext.setProvider(null);
   }



   /***************************************************************************
    ** Test setting and getting config.
    ***************************************************************************/
   @Test
   void testConfigGetterSetter()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY);

      QSessionStoreQBitContext.setConfig(config);

      assertThat(QSessionStoreQBitContext.getConfig()).isSameAs(config);
   }



   /***************************************************************************
    ** Test setting and getting provider.
    ***************************************************************************/
   @Test
   void testProviderGetterSetter()
   {
      InMemorySessionStoreProvider provider = new InMemorySessionStoreProvider();

      QSessionStoreQBitContext.setProvider(provider);

      assertThat(QSessionStoreQBitContext.getProvider()).isSameAs(provider);
   }



   /***************************************************************************
    ** Test getting null config when not set.
    ***************************************************************************/
   @Test
   void testGetConfig_notSet_returnsNull()
   {
      assertThat(QSessionStoreQBitContext.getConfig()).isNull();
   }



   /***************************************************************************
    ** Test getting null provider when not set.
    ***************************************************************************/
   @Test
   void testGetProvider_notSet_returnsNull()
   {
      assertThat(QSessionStoreQBitContext.getProvider()).isNull();
   }

}
