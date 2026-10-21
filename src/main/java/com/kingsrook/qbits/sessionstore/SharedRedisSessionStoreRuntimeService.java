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
import com.kingsrook.qbits.redis.api.RedisAccess;
import com.kingsrook.qbits.redis.api.RedisNamespace;
import com.kingsrook.qbits.sessionstore.providers.SharedRedisSessionStoreProvider;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;


/*******************************************************************************
 ** Own shared session registrations; Redis alone owns the backing connection pool.
 *******************************************************************************/
public class SharedRedisSessionStoreRuntimeService implements QRuntimeServiceInterface
{
   private static final Map<QInstance, Object> OWNERS = new WeakHashMap<>();
   private final Object token = new Object();
   private QInstance instance;
   private QSessionStoreProviderInterface provider;



   /***************************************************************************
    ** Framework lifecycle identity.
    ***************************************************************************/
   @Override
   public String getName()
   {
      return "shared-redis-session-store";
   }



   /***************************************************************************
    ** Require the named Redis pool first; reconstruct registrations on restart.
    ***************************************************************************/
   @Override
   public synchronized void start(QInstance owner) throws QException
   {
      if(owner == null)
      {
         throw new QException("Shared Redis session runtime requires an application");
      }
      synchronized(OWNERS)
      {
         if(instance != null || OWNERS.containsKey(owner))
         {
            throw new QException("Shared Redis session runtime is already started");
         }
         OWNERS.put(owner, token);
      }
      QSessionStoreProviderInterface candidate = QSessionStoreQBitContext.getProvider(owner);
      try
      {
         if(candidate != null && !(candidate instanceof SharedRedisSessionStoreProvider))
         {
            throw new QException("Application has another session provider");
         }
         var config = owner.getQBits().values().stream().map(bit -> bit.getConfig())
            .filter(QSessionStoreQBitConfig.class::isInstance)
            .map(QSessionStoreQBitConfig.class::cast)
            .filter(value -> value.getProviderType() == QSessionStoreProviderType.SHARED_REDIS)
            .findFirst().orElseThrow();
         var settings = config.getSharedRedis();
         RedisAccess.cache(owner, settings.connectionName(), new RedisNamespace(settings.deployment(),
            settings.application(), settings.tenant(), "session-store"), settings.maximumValueBytes());
         var registered = QSessionStoreRegistry.getInstance(owner).getProvider().orElse(null);
         if(registered != null && registered != candidate)
         {
            throw new QException("Application has another session provider");
         }
         if(candidate == null)
         {
            candidate = QSessionStoreProviderFactory.createProvider(owner, config);
         }
         QSessionStoreQBitContext.setConfig(owner, config);
         QSessionStoreQBitContext.setProvider(owner, candidate);
         QSessionStoreRegistry.getInstance(owner).register(candidate);
         instance = owner;
         provider = candidate;
      }
      catch(Exception e)
      {
         if(candidate instanceof SharedRedisSessionStoreProvider)
         {
            QSessionStoreRegistry.getInstance(owner).clearIfSame(candidate);
            QSessionStoreQBitContext.clearIfSame(owner, candidate);
         }
         synchronized(OWNERS)
         {
            OWNERS.remove(owner, token);
         }
         throw new QException("Shared Redis session startup failed; check connection startup and provider ownership");
      }
   }



   /***************************************************************************
    ** Keep registrations available until dependent HTTP and scheduler work stops.
    ***************************************************************************/
   @Override
   public void stop()
   {
   }



   /***************************************************************************
    ** Release only our registrations, with no QContext requirement or pool closure.
    ***************************************************************************/
   @Override
   public synchronized void afterApplicationStop()
   {
      if(instance == null)
      {
         return;
      }
      QSessionStoreRegistry.getInstance(instance).clearIfSame(provider);
      QSessionStoreQBitContext.clearIfSame(instance, provider);
      synchronized(OWNERS)
      {
         OWNERS.remove(instance, token);
      }
      provider = null;
      instance = null;
   }
}
