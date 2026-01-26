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
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for StoredSession entity.
 *******************************************************************************/
class StoredSessionTest
{

   /***************************************************************************
    ** Test field constants.
    ***************************************************************************/
   @Test
   void testFieldConstants()
   {
      assertThat(StoredSession.TABLE_NAME).isEqualTo("storedSession");
      assertThat(StoredSession.FIELD_ID).isEqualTo("id");
      assertThat(StoredSession.FIELD_SESSION_UUID).isEqualTo("sessionUuid");
      assertThat(StoredSession.FIELD_USER_ID).isEqualTo("userId");
      assertThat(StoredSession.FIELD_SESSION_DATA).isEqualTo("sessionData");
      assertThat(StoredSession.FIELD_EXPIRES_AT).isEqualTo("expiresAt");
      assertThat(StoredSession.FIELD_CREATE_DATE).isEqualTo("createDate");
      assertThat(StoredSession.FIELD_MODIFY_DATE).isEqualTo("modifyDate");
   }



   /***************************************************************************
    ** Test fluent setters and getters.
    ***************************************************************************/
   @Test
   void testFluentSettersAndGetters()
   {
      Instant now = Instant.now();

      StoredSession session = new StoredSession()
         .withId(1)
         .withSessionUuid("test-uuid")
         .withUserId("user123")
         .withSessionData("{\"key\":\"value\"}")
         .withExpiresAt(now)
         .withCreateDate(now)
         .withModifyDate(now);

      assertThat(session.getId()).isEqualTo(1);
      assertThat(session.getSessionUuid()).isEqualTo("test-uuid");
      assertThat(session.getUserId()).isEqualTo("user123");
      assertThat(session.getSessionData()).isEqualTo("{\"key\":\"value\"}");
      assertThat(session.getExpiresAt()).isEqualTo(now);
      assertThat(session.getCreateDate()).isEqualTo(now);
      assertThat(session.getModifyDate()).isEqualTo(now);
   }



   /***************************************************************************
    ** Test standard setters.
    ***************************************************************************/
   @Test
   void testStandardSetters()
   {
      Instant now = Instant.now();
      StoredSession session = new StoredSession();

      session.setId(42);
      session.setSessionUuid("uuid-42");
      session.setUserId("user42");
      session.setSessionData("data");
      session.setExpiresAt(now);
      session.setCreateDate(now);
      session.setModifyDate(now);

      assertThat(session.getId()).isEqualTo(42);
      assertThat(session.getSessionUuid()).isEqualTo("uuid-42");
      assertThat(session.getUserId()).isEqualTo("user42");
      assertThat(session.getSessionData()).isEqualTo("data");
      assertThat(session.getExpiresAt()).isEqualTo(now);
      assertThat(session.getCreateDate()).isEqualTo(now);
      assertThat(session.getModifyDate()).isEqualTo(now);
   }

}
