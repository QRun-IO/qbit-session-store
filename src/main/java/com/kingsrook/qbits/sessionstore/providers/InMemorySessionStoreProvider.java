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
package com.kingsrook.qbits.sessionstore.providers;


import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderInterface;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** In-memory session store provider using ConcurrentHashMap with LRU eviction.
 ** Best for: development, testing, single-instance deployments.
 *******************************************************************************/
public class InMemorySessionStoreProvider implements QSessionStoreProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(InMemorySessionStoreProvider.class);

   private Map<String, CachedSession> cache;
   private QSessionStoreQBitConfig    config;
   private Duration                   defaultTtl;



   /***************************************************************************
    ** Internal record to hold session with expiration time.
    ***************************************************************************/
   private record CachedSession(QSession session, Instant expiresAt)
   {
      boolean isExpired()
      {
         return Instant.now().isAfter(expiresAt);
      }
   }



   /***************************************************************************
    ** Configure the provider.
    ***************************************************************************/
   @Override
   public void configure(QSessionStoreQBitConfig config)
   {
      this.config = config;
      this.defaultTtl = config.getDefaultTtl();

      Integer maxSize = config.getMaxCacheSize();
      if(maxSize != null && maxSize > 0)
      {
         //////////////////////////////////////////
         // Use LinkedHashMap for LRU eviction  //
         //////////////////////////////////////////
         this.cache = new ConcurrentHashMap<>(new LinkedHashMap<>(maxSize, 0.75f, true)
         {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedSession> eldest)
            {
               return size() > maxSize;
            }
         });
      }
      else
      {
         this.cache = new ConcurrentHashMap<>();
      }

      LOG.info("InMemory session store configured", logPair("maxSize", maxSize));
   }



   /***************************************************************************
    ** Store a session with the given TTL.
    ***************************************************************************/
   @Override
   public void store(String sessionUuid, QSession session, Duration ttl)
   {
      Duration effectiveTtl = ttl != null ? ttl : defaultTtl;
      Instant expiresAt = Instant.now().plus(effectiveTtl);
      cache.put(sessionUuid, new CachedSession(session, expiresAt));
      LOG.debug("Stored session", logPair("sessionUuid", sessionUuid), logPair("expiresAt", expiresAt));
   }



   /***************************************************************************
    ** Load a session by UUID.
    ***************************************************************************/
   @Override
   public Optional<QSession> load(String sessionUuid)
   {
      CachedSession cached = cache.get(sessionUuid);
      if(cached == null)
      {
         return Optional.empty();
      }

      if(cached.isExpired())
      {
         cache.remove(sessionUuid);
         LOG.debug("Session expired", logPair("sessionUuid", sessionUuid));
         return Optional.empty();
      }

      return Optional.of(cached.session());
   }



   /***************************************************************************
    ** Remove a session by UUID.
    ***************************************************************************/
   @Override
   public void remove(String sessionUuid)
   {
      cache.remove(sessionUuid);
      LOG.debug("Removed session", logPair("sessionUuid", sessionUuid));
   }



   /***************************************************************************
    ** Touch a session to reset its TTL.
    ***************************************************************************/
   @Override
   public void touch(String sessionUuid)
   {
      CachedSession cached = cache.get(sessionUuid);
      if(cached != null && !cached.isExpired())
      {
         Instant newExpiresAt = Instant.now().plus(defaultTtl);
         cache.put(sessionUuid, new CachedSession(cached.session(), newExpiresAt));
         LOG.debug("Touched session", logPair("sessionUuid", sessionUuid), logPair("newExpiresAt", newExpiresAt));
      }
   }



   /***************************************************************************
    ** Get the default TTL for sessions.
    ***************************************************************************/
   @Override
   public Duration getDefaultTtl()
   {
      return defaultTtl;
   }



   /***************************************************************************
    ** Load a session and touch it in a single atomic operation.
    ***************************************************************************/
   @Override
   public Optional<QSession> loadAndTouch(String sessionUuid)
   {
      CachedSession cached = cache.get(sessionUuid);
      if(cached == null)
      {
         return Optional.empty();
      }

      if(cached.isExpired())
      {
         cache.remove(sessionUuid);
         LOG.debug("Session expired", logPair("sessionUuid", sessionUuid));
         return Optional.empty();
      }

      //////////////////////////////////////////////////////////////////////
      // Atomically update the TTL while returning the session            //
      //////////////////////////////////////////////////////////////////////
      Instant newExpiresAt = Instant.now().plus(defaultTtl);
      cache.put(sessionUuid, new CachedSession(cached.session(), newExpiresAt));
      LOG.debug("Loaded and touched session", logPair("sessionUuid", sessionUuid), logPair("newExpiresAt", newExpiresAt));

      return Optional.of(cached.session());
   }



   /***************************************************************************
    ** Clean up expired sessions.
    ***************************************************************************/
   @Override
   public void cleanExpired()
   {
      int removed = 0;
      Iterator<Map.Entry<String, CachedSession>> iterator = cache.entrySet().iterator();
      while(iterator.hasNext())
      {
         Map.Entry<String, CachedSession> entry = iterator.next();
         if(entry.getValue().isExpired())
         {
            iterator.remove();
            removed++;
         }
      }

      if(removed > 0)
      {
         LOG.info("Cleaned expired sessions", logPair("removed", removed), logPair("remaining", cache.size()));
      }
   }



   /***************************************************************************
    ** Get provider status.
    ***************************************************************************/
   @Override
   public String status()
   {
      return "InMemory: " + cache.size() + " sessions";
   }

}
