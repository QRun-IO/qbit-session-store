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
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderType;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit tests for RedisSessionStoreProvider.
 ** Note: These tests verify configuration and error handling without a real Redis.
 ** Integration tests with Testcontainers are in RedisSessionStoreProviderIT.
 *******************************************************************************/
class RedisSessionStoreProviderTest
{

   /***************************************************************************
    ** Test getDefaultTtl returns configured value.
    ***************************************************************************/
   @Test
   void testGetDefaultTtl()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      // Configure with a non-default host to avoid actual connection
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withDefaultTtl(Duration.ofHours(4));

      provider.configure(config);

      assertThat(provider.getDefaultTtl()).isEqualTo(Duration.ofHours(4));
   }



   /***************************************************************************
    ** Test load returns empty when Redis is unavailable.
    ***************************************************************************/
   @Test
   void testLoad_connectionError_returnsEmpty()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should return empty (with logged warning) rather than throwing
      assertThat(provider.load("test-uuid")).isEmpty();
   }



   /***************************************************************************
    ** Test loadAndTouch returns empty when Redis is unavailable.
    ***************************************************************************/
   @Test
   void testLoadAndTouch_connectionError_returnsEmpty()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should return empty (with logged warning) rather than throwing
      assertThat(provider.loadAndTouch("test-uuid")).isEmpty();
   }



   /***************************************************************************
    ** Test store handles connection errors gracefully.
    ***************************************************************************/
   @Test
   void testStore_connectionError_doesNotThrow()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should not throw - just logs warning
      provider.store("test-uuid", new com.kingsrook.qqq.backend.core.model.session.QSession(), null);
   }



   /***************************************************************************
    ** Test remove handles connection errors gracefully.
    ***************************************************************************/
   @Test
   void testRemove_connectionError_doesNotThrow()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should not throw - just logs warning
      provider.remove("test-uuid");
   }



   /***************************************************************************
    ** Test touch handles connection errors gracefully.
    ***************************************************************************/
   @Test
   void testTouch_connectionError_doesNotThrow()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should not throw - just logs warning
      provider.touch("test-uuid");
   }



   /***************************************************************************
    ** Test cleanExpired is a no-op for Redis.
    ***************************************************************************/
   @Test
   void testCleanExpired_isNoOp()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      // Should complete without error (Redis handles TTL natively)
      provider.cleanExpired();
   }



   /***************************************************************************
    ** Test status returns error message when Redis unavailable.
    ***************************************************************************/
   @Test
   void testStatus_connectionError()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withDefaultTtl(Duration.ofHours(1));

      provider.configure(config);

      String status = provider.status();
      assertThat(status).contains("Redis");
      assertThat(status).contains("error");
   }



   /***************************************************************************
    ** Test configure with password.
    ***************************************************************************/
   @Test
   void testConfigure_withPassword()
   {
      RedisSessionStoreProvider provider = new RedisSessionStoreProvider();

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withRedisHost("nonexistent-host-for-test")
         .withRedisPort(6379)
         .withRedisPassword("secret-password")
         .withRedisKeyPrefix("custom:prefix:")
         .withDefaultTtl(Duration.ofHours(2));

      // Should not throw during configuration
      provider.configure(config);

      assertThat(provider.getDefaultTtl()).isEqualTo(Duration.ofHours(2));
   }

}
