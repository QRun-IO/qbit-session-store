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
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
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



   /***************************************************************************
    ** Test validation for TABLE_BASED with backend not found.
    ***************************************************************************/
   @Test
   void testValidate_tableBasedBackendNotFound_addsError()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("nonexistentBackend");
      QInstance qInstance = new QInstance();
      List<String> errors = new ArrayList<>();

      config.validate(qInstance, errors);

      assertThat(errors).contains("Backend not found: nonexistentBackend");
   }



   /***************************************************************************
    ** Test all setters are covered (standard setters).
    ***************************************************************************/
   @Test
   void testStandardSetters()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig();

      config.setProviderType(QSessionStoreProviderType.IN_MEMORY);
      config.setDefaultTtl(Duration.ofMinutes(45));
      config.setEnableSlidingExpiration(false);
      config.setBackendName("backend");
      config.setTableName("table");
      config.setTableNamePrefix("prefix_");
      config.setMaxCacheSize(500);
      config.setRedisHost("host");
      config.setRedisPort(1234);
      config.setRedisPassword("pass");
      config.setRedisKeyPrefix("key:");
      config.setEnableCleanupProcess(false);
      config.setCleanupIntervalSeconds(600);

      assertThat(config.getProviderType()).isEqualTo(QSessionStoreProviderType.IN_MEMORY);
      assertThat(config.getDefaultTtl()).isEqualTo(Duration.ofMinutes(45));
      assertThat(config.getEnableSlidingExpiration()).isFalse();
      assertThat(config.getBackendName()).isEqualTo("backend");
      assertThat(config.getTableName()).isEqualTo("table");
      assertThat(config.getTableNamePrefix()).isEqualTo("prefix_");
      assertThat(config.getMaxCacheSize()).isEqualTo(500);
      assertThat(config.getRedisHost()).isEqualTo("host");
      assertThat(config.getRedisPort()).isEqualTo(1234);
      assertThat(config.getRedisPassword()).isEqualTo("pass");
      assertThat(config.getRedisKeyPrefix()).isEqualTo("key:");
      assertThat(config.getEnableCleanupProcess()).isFalse();
      assertThat(config.getCleanupIntervalSeconds()).isEqualTo(600);
   }



   /***************************************************************************
    ** Test TABLE_BASED fluent setters.
    ***************************************************************************/
   @Test
   void testTableBasedFluentSetters()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.TABLE_BASED)
         .withBackendName("myBackend")
         .withTableName("myTable")
         .withTableNamePrefix("app_")
         .withEnableCleanupProcess(true)
         .withCleanupIntervalSeconds(120);

      assertThat(config.getBackendName()).isEqualTo("myBackend");
      assertThat(config.getTableName()).isEqualTo("myTable");
      assertThat(config.getTableNamePrefix()).isEqualTo("app_");
      assertThat(config.getEnableCleanupProcess()).isTrue();
      assertThat(config.getCleanupIntervalSeconds()).isEqualTo(120);
   }



   /***************************************************************************
    ** Test IN_MEMORY fluent setters.
    ***************************************************************************/
   @Test
   void testInMemoryFluentSetters()
   {
      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.IN_MEMORY)
         .withMaxCacheSize(5000);

      assertThat(config.getMaxCacheSize()).isEqualTo(5000);
   }



   /***************************************************************************
    ** Test CUSTOM provider setters.
    ***************************************************************************/
   @Test
   void testCustomProviderSetters()
   {
      QCodeReference codeRef = new QCodeReference(String.class);

      QSessionStoreQBitConfig config = new QSessionStoreQBitConfig()
         .withProviderType(QSessionStoreProviderType.CUSTOM)
         .withCustomProviderCodeReference(codeRef);

      assertThat(config.getCustomProviderCodeReference()).isSameAs(codeRef);

      // Test standard setter too
      QCodeReference codeRef2 = new QCodeReference(Integer.class);
      config.setCustomProviderCodeReference(codeRef2);
      assertThat(config.getCustomProviderCodeReference()).isSameAs(codeRef2);
   }

}
