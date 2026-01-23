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
 ** Static context holder for the Session Store QBit configuration and provider.
 *******************************************************************************/
public class QSessionStoreQBitContext
{
   private static QSessionStoreQBitConfig         config;
   private static QSessionStoreProviderInterface  provider;



   /***************************************************************************
    ** Getter for config
    ***************************************************************************/
   public static QSessionStoreQBitConfig getConfig()
   {
      return config;
   }



   /***************************************************************************
    ** Setter for config
    ***************************************************************************/
   public static void setConfig(QSessionStoreQBitConfig config)
   {
      QSessionStoreQBitContext.config = config;
   }



   /***************************************************************************
    ** Getter for provider
    ***************************************************************************/
   public static QSessionStoreProviderInterface getProvider()
   {
      return provider;
   }



   /***************************************************************************
    ** Setter for provider
    ***************************************************************************/
   public static void setProvider(QSessionStoreProviderInterface provider)
   {
      QSessionStoreQBitContext.provider = provider;
   }



   /***************************************************************************
    ** Clear all context (useful for testing).
    ***************************************************************************/
   public static void clear()
   {
      config = null;
      provider = null;
   }

}
