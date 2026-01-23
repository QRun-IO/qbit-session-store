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
import java.util.Optional;
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderInterface;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Redis session store provider using Jedis client.
 ** Best for: distributed HA deployments, horizontal scaling.
 *******************************************************************************/
public class RedisSessionStoreProvider implements QSessionStoreProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(RedisSessionStoreProvider.class);

   private QSessionStoreQBitConfig config;
   private JedisPool               jedisPool;
   private String                  keyPrefix;
   private Duration                defaultTtl;



   /***************************************************************************
    ** Configure the provider.
    ***************************************************************************/
   @Override
   public void configure(QSessionStoreQBitConfig config)
   {
      this.config = config;
      this.keyPrefix = config.getRedisKeyPrefix();
      this.defaultTtl = config.getDefaultTtl();

      JedisPoolConfig poolConfig = new JedisPoolConfig();
      poolConfig.setMaxTotal(128);
      poolConfig.setMaxIdle(128);
      poolConfig.setMinIdle(16);
      poolConfig.setTestOnBorrow(true);
      poolConfig.setTestOnReturn(true);
      poolConfig.setTestWhileIdle(true);

      if(StringUtils.hasContent(config.getRedisPassword()))
      {
         this.jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 2000, config.getRedisPassword());
      }
      else
      {
         this.jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort());
      }

      LOG.info("Redis session store configured", logPair("host", config.getRedisHost()), logPair("port", config.getRedisPort()));
   }



   /***************************************************************************
    ** Build the Redis key for a session UUID.
    ***************************************************************************/
   private String buildKey(String sessionUuid)
   {
      return keyPrefix + sessionUuid;
   }



   /***************************************************************************
    ** Store a session with the given TTL.
    ***************************************************************************/
   @Override
   public void store(String sessionUuid, QSession session, Duration ttl)
   {
      try(Jedis jedis = jedisPool.getResource())
      {
         Duration effectiveTtl = ttl != null ? ttl : defaultTtl;
         String key = buildKey(sessionUuid);
         String sessionJson = JsonUtils.toJson(session);

         jedis.setex(key, effectiveTtl.toSeconds(), sessionJson);
         LOG.debug("Stored session in Redis", logPair("sessionUuid", sessionUuid), logPair("ttlSeconds", effectiveTtl.toSeconds()));
      }
      catch(Exception e)
      {
         LOG.warn("Failed to store session in Redis", logPair("sessionUuid", sessionUuid), e);
      }
   }



   /***************************************************************************
    ** Load a session by UUID.
    ***************************************************************************/
   @Override
   public Optional<QSession> load(String sessionUuid)
   {
      try(Jedis jedis = jedisPool.getResource())
      {
         String key = buildKey(sessionUuid);
         String sessionJson = jedis.get(key);

         if(sessionJson == null)
         {
            return Optional.empty();
         }

         QSession session = JsonUtils.toObject(sessionJson, QSession.class);
         return Optional.of(session);
      }
      catch(Exception e)
      {
         LOG.warn("Failed to load session from Redis", logPair("sessionUuid", sessionUuid), e);
         return Optional.empty();
      }
   }



   /***************************************************************************
    ** Remove a session by UUID.
    ***************************************************************************/
   @Override
   public void remove(String sessionUuid)
   {
      try(Jedis jedis = jedisPool.getResource())
      {
         String key = buildKey(sessionUuid);
         jedis.del(key);
         LOG.debug("Removed session from Redis", logPair("sessionUuid", sessionUuid));
      }
      catch(Exception e)
      {
         LOG.warn("Failed to remove session from Redis", logPair("sessionUuid", sessionUuid), e);
      }
   }



   /***************************************************************************
    ** Touch a session to reset its TTL.
    ***************************************************************************/
   @Override
   public void touch(String sessionUuid)
   {
      try(Jedis jedis = jedisPool.getResource())
      {
         String key = buildKey(sessionUuid);
         jedis.expire(key, defaultTtl.toSeconds());
         LOG.debug("Touched session in Redis", logPair("sessionUuid", sessionUuid), logPair("newTtlSeconds", defaultTtl.toSeconds()));
      }
      catch(Exception e)
      {
         LOG.warn("Failed to touch session in Redis", logPair("sessionUuid", sessionUuid), e);
      }
   }



   /***************************************************************************
    ** Clean up expired sessions. Redis handles TTL natively, so this is a no-op.
    ***************************************************************************/
   @Override
   public void cleanExpired()
   {
      // Redis handles TTL-based expiration natively - nothing to do here
      LOG.debug("Redis handles TTL expiration natively - no cleanup needed");
   }



   /***************************************************************************
    ** Get provider status.
    ***************************************************************************/
   @Override
   public String status()
   {
      try(Jedis jedis = jedisPool.getResource())
      {
         String info = jedis.info("keyspace");
         return "Redis: connected to " + config.getRedisHost() + ":" + config.getRedisPort();
      }
      catch(Exception e)
      {
         return "Redis: error - " + e.getMessage();
      }
   }

}
