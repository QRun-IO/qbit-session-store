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


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for QSessionStoreQBitConfig.
 *******************************************************************************/
class QSessionStoreQBitConfigTest
{

   /***************************************************************************
    ** Test validation with missing provider type.
    ***************************************************************************/
   @Test
   void testValidate_missingProviderType_addsError()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig();
      QInstance qInstance = new QInstance();
      List<String> errors = new ArrayList<>();

      config.validate(qInstance, errors);

      assertThat(errors).contains("providerType is required for QSessionStoreQBit");
   }



   /***************************************************************************
    ** Test validation for TABLE_BASED provider missing backend.
    ***************************************************************************/
   @Test
   void testValidate_tableBasedMissingBackend_addsError()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED);
      QInstance qInstance = new QInstance();
      List<String> errors = new ArrayList<>();

      config.validate(qInstance, errors);

      assertThat(errors).contains("backendName is required for TABLE_BASED provider");
   }



   /***************************************************************************
    ** Test validation for REDIS provider missing host.
    ***************************************************************************/
   @Test
   void testValidate_redisMissingHost_addsError()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS);
      QInstance qInstance = new QInstance();
      List<String> errors = new ArrayList<>();

      config.validate(qInstance, errors);

      assertThat(errors).contains("redisHost is required for REDIS provider");
   }



   /***************************************************************************
    ** Test validation for CUSTOM provider missing code reference.
    ***************************************************************************/
   @Test
   void testValidate_customMissingCodeRef_addsError()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.CUSTOM);
      QInstance qInstance = new QInstance();
      List<String> errors = new ArrayList<>();

      config.validate(qInstance, errors);

      assertThat(errors).contains("customProviderCodeReference is required for CUSTOM provider");
   }



   /***************************************************************************
    ** Test validation for IN_MEMORY provider passes.
    ***************************************************************************/
   @Test
   void testValidate_inMemory_noErrors()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY);
      QInstance qInstance = new QInstance();
      List<String> errors = new ArrayList<>();

      config.validate(qInstance, errors);

      assertThat(errors).isEmpty();
   }



   /***************************************************************************
    ** Test default values.
    ***************************************************************************/
   @Test
   void testDefaultValues()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig();

      assertThat(config.getDefaultTtl()).isEqualTo(Duration.ofHours(1));
      assertThat(config.getEnableSlidingExpiration()).isTrue();
      assertThat(config.getTableName()).isEqualTo("storedSession");
      assertThat(config.getMaxCacheSize()).isEqualTo(10000);
      assertThat(config.getRedisPort()).isEqualTo(6379);
      assertThat(config.getRedisKeyPrefix()).isEqualTo("qqq:session:");
      assertThat(config.getEnableCleanupProcess()).isTrue();
      assertThat(config.getCleanupIntervalSeconds()).isEqualTo(300);
   }



   /***************************************************************************
    ** Test fluent setters.
    ***************************************************************************/
   @Test
   void testFluentSetters()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.REDIS)
         .withDefaultTtl(Duration.ofHours(8))
         .withEnableSlidingExpiration(false)
         .withRedisHost("localhost")
         .withRedisPort(6380)
         .withRedisPassword("secret")
         .withRedisKeyPrefix("myapp:session:");

      assertThat(config.getProviderType()).isEqualTo(QSessionStoreProviderType.REDIS);
      assertThat(config.getDefaultTtl()).isEqualTo(Duration.ofHours(8));
      assertThat(config.getEnableSlidingExpiration()).isFalse();
      assertThat(config.getRedisHost()).isEqualTo("localhost");
      assertThat(config.getRedisPort()).isEqualTo(6380);
      assertThat(config.getRedisPassword()).isEqualTo("secret");
      assertThat(config.getRedisKeyPrefix()).isEqualTo("myapp:session:");
   }



   /***************************************************************************
    ** Test applyPrefix with prefix.
    ***************************************************************************/
   @Test
   void testApplyPrefix_withPrefix()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withTableNamePrefix("app_");

      assertThat(config.applyPrefix("storedSession")).isEqualTo("app_storedSession");
   }



   /***************************************************************************
    ** Test applyPrefix without prefix.
    ***************************************************************************/
   @Test
   void testApplyPrefix_noPrefix()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig();

      assertThat(config.applyPrefix("storedSession")).isEqualTo("storedSession");
   }

}
