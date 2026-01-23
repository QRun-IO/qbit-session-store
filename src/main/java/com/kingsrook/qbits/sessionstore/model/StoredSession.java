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
package com.kingsrook.qbits.sessionstore.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Record entity for the StoredSession table.
 *******************************************************************************/
public class StoredSession extends QRecord
{
   public static final String TABLE_NAME = "storedSession";

   public static final String FIELD_ID           = "id";
   public static final String FIELD_SESSION_UUID = "sessionUuid";
   public static final String FIELD_USER_ID      = "userId";
   public static final String FIELD_SESSION_DATA = "sessionData";
   public static final String FIELD_EXPIRES_AT   = "expiresAt";
   public static final String FIELD_CREATE_DATE  = "createDate";
   public static final String FIELD_MODIFY_DATE  = "modifyDate";

   private Integer id;
   private String  sessionUuid;
   private String  userId;
   private String  sessionData;
   private Instant expiresAt;
   private Instant createDate;
   private Instant modifyDate;



   /***************************************************************************
    ** Getter for id
    ***************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /***************************************************************************
    ** Setter for id
    ***************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /***************************************************************************
    ** Fluent setter for id
    ***************************************************************************/
   public StoredSession withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /***************************************************************************
    ** Getter for sessionUuid
    ***************************************************************************/
   public String getSessionUuid()
   {
      return (this.sessionUuid);
   }



   /***************************************************************************
    ** Setter for sessionUuid
    ***************************************************************************/
   public void setSessionUuid(String sessionUuid)
   {
      this.sessionUuid = sessionUuid;
   }



   /***************************************************************************
    ** Fluent setter for sessionUuid
    ***************************************************************************/
   public StoredSession withSessionUuid(String sessionUuid)
   {
      this.sessionUuid = sessionUuid;
      return (this);
   }



   /***************************************************************************
    ** Getter for userId
    ***************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /***************************************************************************
    ** Setter for userId
    ***************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /***************************************************************************
    ** Fluent setter for userId
    ***************************************************************************/
   public StoredSession withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /***************************************************************************
    ** Getter for sessionData
    ***************************************************************************/
   public String getSessionData()
   {
      return (this.sessionData);
   }



   /***************************************************************************
    ** Setter for sessionData
    ***************************************************************************/
   public void setSessionData(String sessionData)
   {
      this.sessionData = sessionData;
   }



   /***************************************************************************
    ** Fluent setter for sessionData
    ***************************************************************************/
   public StoredSession withSessionData(String sessionData)
   {
      this.sessionData = sessionData;
      return (this);
   }



   /***************************************************************************
    ** Getter for expiresAt
    ***************************************************************************/
   public Instant getExpiresAt()
   {
      return (this.expiresAt);
   }



   /***************************************************************************
    ** Setter for expiresAt
    ***************************************************************************/
   public void setExpiresAt(Instant expiresAt)
   {
      this.expiresAt = expiresAt;
   }



   /***************************************************************************
    ** Fluent setter for expiresAt
    ***************************************************************************/
   public StoredSession withExpiresAt(Instant expiresAt)
   {
      this.expiresAt = expiresAt;
      return (this);
   }



   /***************************************************************************
    ** Getter for createDate
    ***************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /***************************************************************************
    ** Setter for createDate
    ***************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /***************************************************************************
    ** Fluent setter for createDate
    ***************************************************************************/
   public StoredSession withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /***************************************************************************
    ** Getter for modifyDate
    ***************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /***************************************************************************
    ** Setter for modifyDate
    ***************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /***************************************************************************
    ** Fluent setter for modifyDate
    ***************************************************************************/
   public StoredSession withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
