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


import java.util.Map;
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** Application-scoped configuration and provider access for session-store actions.
 *******************************************************************************/
public class QSessionStoreQBitContext
{
   private static final Map<QInstance, QSessionStoreQBitConfig> CONFIGS = new WeakHashMap<>();
   private static final Map<QInstance, QSessionStoreProviderInterface> PROVIDERS = new WeakHashMap<>();



   /***************************************************************************
    ** Get configuration for the current application.
    ***************************************************************************/
   public static QSessionStoreQBitConfig getConfig()
   {
      return getConfig(QContext.getQInstance());
   }



   /***************************************************************************
    ** Get configuration for an explicitly selected application.
    ***************************************************************************/
   public static synchronized QSessionStoreQBitConfig getConfig(QInstance instance)
   {
      return CONFIGS.get(instance);
   }



   /***************************************************************************
    ** Set configuration for the current application.
    ***************************************************************************/
   public static void setConfig(QSessionStoreQBitConfig config)
   {
      setConfig(QContext.getQInstance(), config);
   }



   /***************************************************************************
    ** Register configuration during application metadata production.
    ***************************************************************************/
   public static synchronized void setConfig(QInstance instance, QSessionStoreQBitConfig config)
   {
      if(config == null)
      {
         CONFIGS.remove(instance);
      }
      else
      {
         CONFIGS.put(instance, config);
      }
   }



   /***************************************************************************
    ** Get the current application's provider; never use another application's.
    ***************************************************************************/
   public static QSessionStoreProviderInterface getProvider()
   {
      return getProvider(QContext.getQInstance());
   }



   /***************************************************************************
    ** Get an application's provider explicitly without changing thread context.
    ***************************************************************************/
   public static synchronized QSessionStoreProviderInterface getProvider(QInstance instance)
   {
      return PROVIDERS.get(instance);
   }



   /***************************************************************************
    ** Set the current application's provider.
    ***************************************************************************/
   public static void setProvider(QSessionStoreProviderInterface provider)
   {
      setProvider(QContext.getQInstance(), provider);
   }



   /***************************************************************************
    ** Register a provider for its owning application during metadata production.
    ***************************************************************************/
   public static synchronized void setProvider(QInstance instance, QSessionStoreProviderInterface provider)
   {
      if(provider == null)
      {
         PROVIDERS.remove(instance);
      }
      else
      {
         PROVIDERS.put(instance, provider);
      }
   }



   /***************************************************************************
    ** Clear only the current application's context.
    ***************************************************************************/
   public static void clear()
   {
      clear(QContext.getQInstance());
   }



   /***************************************************************************
    ** Clear only the explicitly selected application's context.
    ***************************************************************************/
   public static synchronized void clear(QInstance instance)
   {
      CONFIGS.remove(instance);
      PROVIDERS.remove(instance);
   }
   /***************************************************************************
    ** Clear shutdown registrations only if they still belong to this provider.
    ***************************************************************************/
   public static synchronized boolean clearIfSame(QInstance instance, QSessionStoreProviderInterface expected)
   {
      if(PROVIDERS.get(instance) != expected)
      {
         return false;
      }
      clear(instance);
      return true;
   }
}
