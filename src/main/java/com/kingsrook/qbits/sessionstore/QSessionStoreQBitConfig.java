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
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Configuration for the Session Store QBit.
 *******************************************************************************/
public class QSessionStoreQBitConfig implements QBitConfig
{
   private QSessionStoreProviderType providerType;
   private Duration                  defaultTtl                   = Duration.ofHours(1);
   private Boolean                   enableSlidingExpiration      = true;
   private String                    backendName;
   private String                    tableName                    = "storedSession";
   private String                    tableNamePrefix;
   private Integer                   maxCacheSize                 = 10000;
   private String                    redisHost;
   private Integer                   redisPort                    = 6379;
   private String                    redisPassword;
   private String                    redisKeyPrefix               = "qqq:session:";
   private QCodeReference            customProviderCodeReference;
   private Boolean                   enableCleanupProcess         = true;
   private Integer                   cleanupIntervalSeconds       = 300;



   /***************************************************************************
    ** Validate configuration before QBit is produced.
    ***************************************************************************/
   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      if(providerType == null)
      {
         errors.add("providerType is required for QSessionStoreQBit");
      }

      if(providerType == QSessionStoreProviderType.TABLE_BASED)
      {
         if(!StringUtils.hasContent(backendName))
         {
            errors.add("backendName is required for TABLE_BASED provider");
         }
         else if(qInstance.getBackend(backendName) == null)
         {
            errors.add("Backend not found: " + backendName);
         }
      }

      if(providerType == QSessionStoreProviderType.REDIS)
      {
         if(!StringUtils.hasContent(redisHost))
         {
            errors.add("redisHost is required for REDIS provider");
         }
      }

      if(providerType == QSessionStoreProviderType.CUSTOM)
      {
         if(customProviderCodeReference == null)
         {
            errors.add("customProviderCodeReference is required for CUSTOM provider");
         }
      }
   }



   /***************************************************************************
    ** Apply table name prefix if configured.
    ***************************************************************************/
   public String applyPrefix(String name)
   {
      if(StringUtils.hasContent(tableNamePrefix))
      {
         return tableNamePrefix + name;
      }
      return name;
   }



   /***************************************************************************
    ** Getter for providerType
    ***************************************************************************/
   public QSessionStoreProviderType getProviderType()
   {
      return (this.providerType);
   }



   /***************************************************************************
    ** Setter for providerType
    ***************************************************************************/
   public void setProviderType(QSessionStoreProviderType providerType)
   {
      this.providerType = providerType;
   }



   /***************************************************************************
    ** Fluent setter for providerType
    ***************************************************************************/
   public QSessionStoreQBitConfig withProviderType(QSessionStoreProviderType providerType)
   {
      this.providerType = providerType;
      return (this);
   }



   /***************************************************************************
    ** Getter for defaultTtl
    ***************************************************************************/
   public Duration getDefaultTtl()
   {
      return (this.defaultTtl);
   }



   /***************************************************************************
    ** Setter for defaultTtl
    ***************************************************************************/
   public void setDefaultTtl(Duration defaultTtl)
   {
      this.defaultTtl = defaultTtl;
   }



   /***************************************************************************
    ** Fluent setter for defaultTtl
    ***************************************************************************/
   public QSessionStoreQBitConfig withDefaultTtl(Duration defaultTtl)
   {
      this.defaultTtl = defaultTtl;
      return (this);
   }



   /***************************************************************************
    ** Getter for enableSlidingExpiration
    ***************************************************************************/
   public Boolean getEnableSlidingExpiration()
   {
      return (this.enableSlidingExpiration);
   }



   /***************************************************************************
    ** Setter for enableSlidingExpiration
    ***************************************************************************/
   public void setEnableSlidingExpiration(Boolean enableSlidingExpiration)
   {
      this.enableSlidingExpiration = enableSlidingExpiration;
   }



   /***************************************************************************
    ** Fluent setter for enableSlidingExpiration
    ***************************************************************************/
   public QSessionStoreQBitConfig withEnableSlidingExpiration(Boolean enableSlidingExpiration)
   {
      this.enableSlidingExpiration = enableSlidingExpiration;
      return (this);
   }



   /***************************************************************************
    ** Getter for backendName
    ***************************************************************************/
   public String getBackendName()
   {
      return (this.backendName);
   }



   /***************************************************************************
    ** Setter for backendName
    ***************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }



   /***************************************************************************
    ** Fluent setter for backendName
    ***************************************************************************/
   public QSessionStoreQBitConfig withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /***************************************************************************
    ** Getter for tableName
    ***************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /***************************************************************************
    ** Setter for tableName
    ***************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /***************************************************************************
    ** Fluent setter for tableName
    ***************************************************************************/
   public QSessionStoreQBitConfig withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /***************************************************************************
    ** Getter for tableNamePrefix
    ***************************************************************************/
   public String getTableNamePrefix()
   {
      return (this.tableNamePrefix);
   }



   /***************************************************************************
    ** Setter for tableNamePrefix
    ***************************************************************************/
   public void setTableNamePrefix(String tableNamePrefix)
   {
      this.tableNamePrefix = tableNamePrefix;
   }



   /***************************************************************************
    ** Fluent setter for tableNamePrefix
    ***************************************************************************/
   public QSessionStoreQBitConfig withTableNamePrefix(String tableNamePrefix)
   {
      this.tableNamePrefix = tableNamePrefix;
      return (this);
   }



   /***************************************************************************
    ** Getter for maxCacheSize
    ***************************************************************************/
   public Integer getMaxCacheSize()
   {
      return (this.maxCacheSize);
   }



   /***************************************************************************
    ** Setter for maxCacheSize
    ***************************************************************************/
   public void setMaxCacheSize(Integer maxCacheSize)
   {
      this.maxCacheSize = maxCacheSize;
   }



   /***************************************************************************
    ** Fluent setter for maxCacheSize
    ***************************************************************************/
   public QSessionStoreQBitConfig withMaxCacheSize(Integer maxCacheSize)
   {
      this.maxCacheSize = maxCacheSize;
      return (this);
   }



   /***************************************************************************
    ** Getter for redisHost
    ***************************************************************************/
   public String getRedisHost()
   {
      return (this.redisHost);
   }



   /***************************************************************************
    ** Setter for redisHost
    ***************************************************************************/
   public void setRedisHost(String redisHost)
   {
      this.redisHost = redisHost;
   }



   /***************************************************************************
    ** Fluent setter for redisHost
    ***************************************************************************/
   public QSessionStoreQBitConfig withRedisHost(String redisHost)
   {
      this.redisHost = redisHost;
      return (this);
   }



   /***************************************************************************
    ** Getter for redisPort
    ***************************************************************************/
   public Integer getRedisPort()
   {
      return (this.redisPort);
   }



   /***************************************************************************
    ** Setter for redisPort
    ***************************************************************************/
   public void setRedisPort(Integer redisPort)
   {
      this.redisPort = redisPort;
   }



   /***************************************************************************
    ** Fluent setter for redisPort
    ***************************************************************************/
   public QSessionStoreQBitConfig withRedisPort(Integer redisPort)
   {
      this.redisPort = redisPort;
      return (this);
   }



   /***************************************************************************
    ** Getter for redisPassword
    ***************************************************************************/
   public String getRedisPassword()
   {
      return (this.redisPassword);
   }



   /***************************************************************************
    ** Setter for redisPassword
    ***************************************************************************/
   public void setRedisPassword(String redisPassword)
   {
      this.redisPassword = redisPassword;
   }



   /***************************************************************************
    ** Fluent setter for redisPassword
    ***************************************************************************/
   public QSessionStoreQBitConfig withRedisPassword(String redisPassword)
   {
      this.redisPassword = redisPassword;
      return (this);
   }



   /***************************************************************************
    ** Getter for redisKeyPrefix
    ***************************************************************************/
   public String getRedisKeyPrefix()
   {
      return (this.redisKeyPrefix);
   }



   /***************************************************************************
    ** Setter for redisKeyPrefix
    ***************************************************************************/
   public void setRedisKeyPrefix(String redisKeyPrefix)
   {
      this.redisKeyPrefix = redisKeyPrefix;
   }



   /***************************************************************************
    ** Fluent setter for redisKeyPrefix
    ***************************************************************************/
   public QSessionStoreQBitConfig withRedisKeyPrefix(String redisKeyPrefix)
   {
      this.redisKeyPrefix = redisKeyPrefix;
      return (this);
   }



   /***************************************************************************
    ** Getter for customProviderCodeReference
    ***************************************************************************/
   public QCodeReference getCustomProviderCodeReference()
   {
      return (this.customProviderCodeReference);
   }



   /***************************************************************************
    ** Setter for customProviderCodeReference
    ***************************************************************************/
   public void setCustomProviderCodeReference(QCodeReference customProviderCodeReference)
   {
      this.customProviderCodeReference = customProviderCodeReference;
   }



   /***************************************************************************
    ** Fluent setter for customProviderCodeReference
    ***************************************************************************/
   public QSessionStoreQBitConfig withCustomProviderCodeReference(QCodeReference customProviderCodeReference)
   {
      this.customProviderCodeReference = customProviderCodeReference;
      return (this);
   }



   /***************************************************************************
    ** Getter for enableCleanupProcess
    ***************************************************************************/
   public Boolean getEnableCleanupProcess()
   {
      return (this.enableCleanupProcess);
   }



   /***************************************************************************
    ** Setter for enableCleanupProcess
    ***************************************************************************/
   public void setEnableCleanupProcess(Boolean enableCleanupProcess)
   {
      this.enableCleanupProcess = enableCleanupProcess;
   }



   /***************************************************************************
    ** Fluent setter for enableCleanupProcess
    ***************************************************************************/
   public QSessionStoreQBitConfig withEnableCleanupProcess(Boolean enableCleanupProcess)
   {
      this.enableCleanupProcess = enableCleanupProcess;
      return (this);
   }



   /***************************************************************************
    ** Getter for cleanupIntervalSeconds
    ***************************************************************************/
   public Integer getCleanupIntervalSeconds()
   {
      return (this.cleanupIntervalSeconds);
   }



   /***************************************************************************
    ** Setter for cleanupIntervalSeconds
    ***************************************************************************/
   public void setCleanupIntervalSeconds(Integer cleanupIntervalSeconds)
   {
      this.cleanupIntervalSeconds = cleanupIntervalSeconds;
   }



   /***************************************************************************
    ** Fluent setter for cleanupIntervalSeconds
    ***************************************************************************/
   public QSessionStoreQBitConfig withCleanupIntervalSeconds(Integer cleanupIntervalSeconds)
   {
      this.cleanupIntervalSeconds = cleanupIntervalSeconds;
      return (this);
   }

}
