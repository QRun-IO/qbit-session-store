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


import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qbits.sessionstore.QSessionStoreProviderInterface;
import com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig;
import com.kingsrook.qbits.sessionstore.model.StoredSession;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Table-based session store provider using QQQ table actions.
 ** Best for: multi-instance deployments, persistence across restarts.
 *******************************************************************************/
public class TableBasedSessionStoreProvider implements QSessionStoreProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(TableBasedSessionStoreProvider.class);

   private QSessionStoreQBitConfig config;
   private String                  tableName;
   private Duration                defaultTtl;



   /***************************************************************************
    ** Configure the provider.
    ***************************************************************************/
   @Override
   public void configure(QSessionStoreQBitConfig config)
   {
      this.config = config;
      this.tableName = config.applyPrefix(config.getTableName());
      this.defaultTtl = config.getDefaultTtl();
      LOG.info("TableBased session store configured", logPair("tableName", tableName));
   }



   /***************************************************************************
    ** Store a session with the given TTL.
    ***************************************************************************/
   @Override
   public void store(String sessionUuid, QSession session, Duration ttl)
   {
      try
      {
         Duration effectiveTtl = ttl != null ? ttl : defaultTtl;
         Instant expiresAt = Instant.now().plus(effectiveTtl);
         String sessionJson = JsonUtils.toJson(session);

         QRecord record = new QRecord()
            .withValue(StoredSession.FIELD_SESSION_UUID, sessionUuid)
            .withValue(StoredSession.FIELD_USER_ID, session.getUser() != null ? session.getUser().getIdReference() : null)
            .withValue(StoredSession.FIELD_SESSION_DATA, sessionJson)
            .withValue(StoredSession.FIELD_EXPIRES_AT, expiresAt);

         runWithSystemSession(() ->
         {
            InsertInput insertInput = new InsertInput();
            insertInput.setTableName(tableName);
            insertInput.setRecords(List.of(record));
            new InsertAction().execute(insertInput);
            return null;
         });

         LOG.debug("Stored session", logPair("sessionUuid", sessionUuid), logPair("expiresAt", expiresAt));
      }
      catch(Exception e)
      {
         LOG.warn("Failed to store session", logPair("sessionUuid", sessionUuid), e);
      }
   }



   /***************************************************************************
    ** Load a session by UUID.
    ***************************************************************************/
   @Override
   public Optional<QSession> load(String sessionUuid)
   {
      try
      {
         QRecord record = runWithSystemSession(() ->
         {
            GetInput getInput = new GetInput();
            getInput.setTableName(tableName);
            getInput.setUniqueKey(Map.of(StoredSession.FIELD_SESSION_UUID, sessionUuid));
            GetOutput getOutput = new GetAction().execute(getInput);
            return getOutput.getRecord();
         });

         if(record == null)
         {
            return Optional.empty();
         }

         Instant expiresAt = record.getValueInstant(StoredSession.FIELD_EXPIRES_AT);
         if(expiresAt != null && Instant.now().isAfter(expiresAt))
         {
            remove(sessionUuid);
            LOG.debug("Session expired", logPair("sessionUuid", sessionUuid));
            return Optional.empty();
         }

         String sessionJson = record.getValueString(StoredSession.FIELD_SESSION_DATA);
         QSession session = JsonUtils.toObject(sessionJson, QSession.class);
         return Optional.of(session);
      }
      catch(Exception e)
      {
         LOG.warn("Failed to load session", logPair("sessionUuid", sessionUuid), e);
         return Optional.empty();
      }
   }



   /***************************************************************************
    ** Remove a session by UUID.
    ***************************************************************************/
   @Override
   public void remove(String sessionUuid)
   {
      try
      {
         runWithSystemSession(() ->
         {
            DeleteInput deleteInput = new DeleteInput();
            deleteInput.setTableName(tableName);
            deleteInput.setQueryFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(StoredSession.FIELD_SESSION_UUID, QCriteriaOperator.EQUALS, sessionUuid)));
            new DeleteAction().execute(deleteInput);
            return null;
         });

         LOG.debug("Removed session", logPair("sessionUuid", sessionUuid));
      }
      catch(Exception e)
      {
         LOG.warn("Failed to remove session", logPair("sessionUuid", sessionUuid), e);
      }
   }



   /***************************************************************************
    ** Touch a session to reset its TTL.
    ***************************************************************************/
   @Override
   public void touch(String sessionUuid)
   {
      try
      {
         Instant newExpiresAt = Instant.now().plus(defaultTtl);

         runWithSystemSession(() ->
         {
            /////////////////////////////////////////////////////////////////
            // First get the record to find its primary key               //
            /////////////////////////////////////////////////////////////////
            GetInput getInput = new GetInput();
            getInput.setTableName(tableName);
            getInput.setUniqueKey(Map.of(StoredSession.FIELD_SESSION_UUID, sessionUuid));
            GetOutput getOutput = new GetAction().execute(getInput);

            if(getOutput.getRecord() != null)
            {
               QRecord updateRecord = new QRecord()
                  .withValue("id", getOutput.getRecord().getValue("id"))
                  .withValue(StoredSession.FIELD_EXPIRES_AT, newExpiresAt);

               UpdateInput updateInput = new UpdateInput();
               updateInput.setTableName(tableName);
               updateInput.setRecords(List.of(updateRecord));
               new UpdateAction().execute(updateInput);
            }
            return null;
         });

         LOG.debug("Touched session", logPair("sessionUuid", sessionUuid), logPair("newExpiresAt", newExpiresAt));
      }
      catch(Exception e)
      {
         LOG.warn("Failed to touch session", logPair("sessionUuid", sessionUuid), e);
      }
   }



   /***************************************************************************
    ** Clean up expired sessions.
    ***************************************************************************/
   @Override
   public void cleanExpired()
   {
      try
      {
         runWithSystemSession(() ->
         {
            DeleteInput deleteInput = new DeleteInput();
            deleteInput.setTableName(tableName);
            deleteInput.setQueryFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(StoredSession.FIELD_EXPIRES_AT, QCriteriaOperator.LESS_THAN, Instant.now())));
            new DeleteAction().execute(deleteInput);
            return null;
         });

         LOG.info("Cleaned expired sessions from table");
      }
      catch(Exception e)
      {
         LOG.warn("Failed to clean expired sessions", e);
      }
   }



   /***************************************************************************
    ** Get provider status.
    ***************************************************************************/
   @Override
   public String status()
   {
      try
      {
         long count = runWithSystemSession(() ->
         {
            QueryInput queryInput = new QueryInput();
            queryInput.setTableName(tableName);
            queryInput.setFilter(new QQueryFilter());
            QueryOutput queryOutput = new QueryAction().execute(queryInput);
            return (long) queryOutput.getRecords().size();
         });
         return "TableBased: " + count + " sessions in " + tableName;
      }
      catch(Exception e)
      {
         return "TableBased: error - " + e.getMessage();
      }
   }



   /***************************************************************************
    ** Run an operation with a system user session.
    ***************************************************************************/
   private <T> T runWithSystemSession(QContextOperation<T> operation) throws QException
   {
      QSession originalSession = QContext.getQSession();
      try
      {
         QContext.setQSession(new QSystemUserSession());
         return operation.run();
      }
      finally
      {
         QContext.setQSession(originalSession);
      }
   }



   /***************************************************************************
    ** Functional interface for operations that need system session context.
    ***************************************************************************/
   @FunctionalInterface
   private interface QContextOperation<T>
   {
      T run() throws QException;
   }

}
