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


import java.nio.charset.StandardCharsets;


/*******************************************************************************
 ** Explicit shared Redis ownership and namespace; credentials stay in Redis runtime.
 *******************************************************************************/
public record SharedRedisSessionStoreSettings(String connectionName, String deployment, String application, String tenant, Integer maximumValueBytes)
{
   /***************************************************************************
    ** Bound namespace inputs and serialized session payloads before registration.
    ***************************************************************************/
   public SharedRedisSessionStoreSettings
   {
      for(String value : new String[] {connectionName, deployment, application, tenant})
      {
         if(value == null || value.isBlank() || value.getBytes(StandardCharsets.UTF_8).length > 256)
         {
            throw new IllegalArgumentException("Shared Redis connection and scope must be nonblank and at most 256 bytes");
         }
      }
      if(maximumValueBytes == null || maximumValueBytes < 1 || maximumValueBytes > 1048576)
      {
         throw new IllegalArgumentException("Shared Redis session size must be between 1 and 1048576 bytes");
      }
   }
}
