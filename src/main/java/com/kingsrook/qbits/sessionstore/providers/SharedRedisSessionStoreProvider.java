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


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qbits.redis.api.RedisAccess;
import com.kingsrook.qbits.redis.api.RedisCache;
import com.kingsrook.qbits.redis.api.RedisNamespace;
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderInterface;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qbits.sessionstore.SharedRedisSessionStoreSettings;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Session rules over application-owned Redis infrastructure. Never owns a pool.
 *******************************************************************************/
public final class SharedRedisSessionStoreProvider implements QSessionStoreProviderInterface
{
   private final WeakReference<QInstance> instance;
   private SharedRedisSessionStoreSettings settings;
   private RedisNamespace namespace;
   private Duration defaultTtl;
   private boolean sliding;



   /***************************************************************************
    ** Bind to the explicit owner, independent of the request thread's context.
    ***************************************************************************/
   public SharedRedisSessionStoreProvider(QInstance instance)
   {
      this.instance = new WeakReference<>(Objects.requireNonNull(instance, "Shared Redis requires an owning QInstance"));
   }



   /***************************************************************************
    ** Do not retain an abandoned application through a static registry value.
    ***************************************************************************/
   private QInstance owner()
   {
      QInstance owner = instance.get();
      if(owner == null)
      {
         throw new IllegalStateException("Owning application is no longer available");
      }
      return owner;
   }



   /***************************************************************************
    ** Validate metadata without creating infrastructure; install Redis first.
    ***************************************************************************/
   @Override
   public void configure(QSessionStoreQBitConfig config)
   {
      if(settings != null)
      {
         throw new IllegalStateException("Shared Redis session provider is already configured");
      }
      var configured = Objects.requireNonNull(config.getSharedRedis());
      Duration ttl = validTtl(config.getDefaultTtl());
      if(!RedisAccess.isConfigured(owner(), configured.connectionName()))
      {
         throw new IllegalArgumentException("Shared Redis session connection is not configured on the owning application");
      }
      namespace = new RedisNamespace(configured.deployment(), configured.application(), configured.tenant(), "session-store");
      defaultTtl = ttl;
      sliding = Boolean.TRUE.equals(config.getEnableSlidingExpiration());
      settings = configured;
   }



   /***************************************************************************
    ** Resolve only started owner infrastructure; never allocate a fallback pool.
    ***************************************************************************/
   private RedisCache cache()
   {
      if(settings == null)
      {
         throw new IllegalStateException("Shared Redis session provider is not configured");
      }
      return RedisAccess.cache(owner(), settings.connectionName(), namespace, settings.maximumValueBytes());
   }



   /***************************************************************************
    ** Serialize within session-store and atomically set value plus expiry.
    ***************************************************************************/
   @Override
   public void store(String id, QSession session, Duration ttl)
   {
      cache().put(id, JsonUtils.toJson(Objects.requireNonNull(session)).getBytes(StandardCharsets.UTF_8), validTtl(ttl == null ? defaultTtl : ttl));
   }



   /***************************************************************************
    ** Absence is a cache miss; infrastructure and decoding errors propagate.
    ***************************************************************************/
   @Override
   public Optional<QSession> load(String id)
   {
      return decode(cache().get(id));
   }



   /***************************************************************************
    ** Failed eviction must propagate so logout can retry rather than claim success.
    ***************************************************************************/
   @Override
   public void remove(String id)
   {
      cache().remove(id);
   }



   /***************************************************************************
    ** Explicit renewal never recreates an expired or deleted session.
    ***************************************************************************/
   @Override
   public void touch(String id)
   {
      cache().touch(id, defaultTtl);
   }



   /***************************************************************************
    ** Respect sliding-expiration configuration with one atomic read/renew command.
    ***************************************************************************/
   @Override
   public Optional<QSession> loadAndTouch(String id)
   {
      return decode(sliding ? cache().getAndTouch(id, defaultTtl) : cache().get(id));
   }



   /***************************************************************************
    ** Bound decoded payloads and never include session contents in error messages.
    ***************************************************************************/
   private Optional<QSession> decode(Optional<byte[]> value)
   {
      return value.map(bytes ->
      {
         if(bytes.length > settings.maximumValueBytes())
         {
            throw new IllegalStateException("Stored session exceeds its configured size limit");
         }
         try
         {
            return Objects.requireNonNull(JsonUtils.toObject(new String(bytes, StandardCharsets.UTF_8), QSession.class));
         }
         catch(IOException | RuntimeException e)
         {
            throw new IllegalStateException("Stored session could not be decoded");
         }
      });
   }



   /***************************************************************************
    ** Bound TTL arithmetic and prevent immortal or truncated sub-millisecond data.
    ***************************************************************************/
   private static Duration validTtl(Duration ttl)
   {
      if(ttl == null || ttl.compareTo(Duration.ofMillis(1)) < 0 || ttl.compareTo(Duration.ofDays(365)) > 0)
      {
         throw new IllegalArgumentException("Session TTL must be between one millisecond and 365 days");
      }
      return ttl;
   }



   /***************************************************************************
    ** Report the configured session lifetime.
    ***************************************************************************/
   @Override
   public Duration getDefaultTtl()
   {
      return defaultTtl;
   }



   /***************************************************************************
    ** Redis owns expiration; no scans or wildcard deletes are needed.
    ***************************************************************************/
   @Override
   public void cleanExpired()
   {
   }



   /***************************************************************************
    ** Health truth belongs to the shared runtime's framework health checks.
    ***************************************************************************/
   @Override
   public String status()
   {
      return "Shared Redis: health is reported by the Redis runtime";
   }
}
