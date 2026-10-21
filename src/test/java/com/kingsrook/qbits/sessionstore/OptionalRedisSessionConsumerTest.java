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


import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/*******************************************************************************
 ** Prove memory consumers can start without either optional Redis artifact.
 *******************************************************************************/
class OptionalRedisSessionConsumerTest
{
   /***************************************************************************
    ** Platform parent prevents optional classes leaking from the Maven runner.
    ***************************************************************************/
   @Test
   void memoryConsumerWithoutRedis() throws Exception
   {
      String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
      URL[] urls = Arrays.stream(classpath.split(java.io.File.pathSeparator))
         .filter(entry -> !entry.replace('\\', '/').contains("/qbit-redis/")
            && !Path.of(entry).getFileName().toString().startsWith("jedis-"))
         .map(entry ->
         {
            try
            {
               return Path.of(entry).toUri().toURL();
            }
            catch(Exception e)
            {
               throw new IllegalArgumentException(e);
            }
         }).toArray(URL[]::new);
      ClassLoader previous = Thread.currentThread().getContextClassLoader();
      try(var loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader()))
      {
         assertThrows(ClassNotFoundException.class,
            () -> loader.loadClass("com.kingsrook.qbits.redis.api.RedisAccess"));
         assertThrows(ClassNotFoundException.class,
            () -> loader.loadClass("redis.clients.jedis.RedisClient"));
         Thread.currentThread().setContextClassLoader(loader);
         loader.loadClass(Probe.class.getName()).getMethod("exercise").invoke(null);
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(previous);
      }
   }



   /***************************************************************************
    ** Loaded with session-store and core but without Redis classes.
    ***************************************************************************/
   public static class Probe
   {
      /************************************************************************
       ** Producer, registry and in-memory storage remain usable independently.
       ************************************************************************/
      public static void exercise() throws Exception
      {
         var instance = new com.kingsrook.qqq.backend.core.model.metadata.QInstance();
         try
         {
            new QSessionStoreQBitProducer().withConfig(new QSessionStoreQBitConfig()
               .withProviderType(QSessionStoreProviderType.IN_MEMORY).withEnableCleanupProcess(false))
               .produce(instance, "session-store");
            var provider = QSessionStoreQBitContext.getProvider(instance);
            provider.store("id", new com.kingsrook.qqq.backend.core.model.session.QSession()
               .withValue("marker", "memory"), java.time.Duration.ofSeconds(30));
            assertEquals("memory", provider.load("id").orElseThrow().getValue("marker"));
            provider.remove("id");
            assertTrue(provider.load("id").isEmpty());
         }
         finally
         {
            QSessionStoreQBitContext.clear(instance);
            com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry.getInstance(instance).clear();
         }
      }
   }
}
