/*
 * JBoss, Home of Professional Open Source.
 * Copyright 200*, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.web.tomcat.service.session;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Random;

import org.apache.catalina.Globals;
import org.jboss.logging.Logger;

/**
 * Unique session id generator.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 */
public class SessionIDGenerator
{
   public static final String DEFAULT_SESSION_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-_";
   public static final String DEFAULT_RANDOM_FILE = "/dev/urandom";
   public static final String DEFAULT_RANDOM_CLASS = SecureRandom.class.getName();
   public static final int    SESSION_ID_BYTES = 16; // We want 16 Bytes for the session-id
   public static final String SESSION_ID_HASH_ALGORITHM = "MD5";
   
   private static char[] sessionIdAlphabet = DEFAULT_SESSION_ID_ALPHABET.toCharArray();
   
   protected Logger log = Logger.getLogger(SessionIDGenerator.class);

   private String randomFile = DEFAULT_RANDOM_FILE;
   private String randomClass = DEFAULT_RANDOM_CLASS;
   private String algorithm = SESSION_ID_HASH_ALGORITHM;
   private String entropy;   
   private DataInputStream randomIS;  
   private MessageDigest digest;
   private Random random; 
   
   // ------------------------------------------------------------  Static

   /**
    * The SessionIdAlphabet is the set of characters used to create a session Id
    */
   public static void setSessionIdAlphabet(String alphabet) 
   {
      if (alphabet.length() != 65) {
         throw new IllegalArgumentException("SessionIdAlphabet must be exactly 65 characters long");
      }

      checkDuplicateChars(alphabet);

      SessionIDGenerator.sessionIdAlphabet = alphabet.toCharArray();
   }

   protected static void checkDuplicateChars(String sessionIdAlphabet) {
      char[] alphabet = sessionIdAlphabet.toCharArray();
      for (int i=0; i < alphabet.length; i++) {
          if (!uniqueChar(alphabet[i], sessionIdAlphabet)) {
              throw new IllegalArgumentException("All chars in SessionIdAlphabet must be unique");
          }
      }
   }
      
   // does a character appear in the String once and only once?
   protected static boolean uniqueChar(char c, String s) {
       int firstIndex = s.indexOf(c);
       if (firstIndex == -1) return false;
       return s.indexOf(c, firstIndex + 1) == -1;
   }

   /**
    * The SessionIdAlphabet is the set of characters used to create a session Id
    */
   public static String getSessionIdAlphabet() {
      return new String(sessionIdAlphabet);
   }   
   
   // ------------------------------------------------------------  Properties
   
   public String getAlgorithm()
   {
      return algorithm;
   }

   public void setAlgorithm(String algorithm)
   {
      this.algorithm = algorithm;
   }

   /**
    * Return the entropy increaser value, or compute a semi-useful value
    * if this String has not yet been set.
    */
   public String getEntropy()
   {
      // Calculate a semi-useful value if this has not been set
      if (this.entropy == null)
      {
         // First, try to use APR to get a crypto secure entropy value         
         try
         {
            byte[] result = new byte[32];
            Class<?> jniOSClazz = Class.forName("org.apache.tomcat.jni.OS");
            Class<?> paramTypes[] = new Class[]{ result.getClass(), int.class };
            Object paramValues[] = new Object[]{ result, Integer.valueOf(32) };
            jniOSClazz.getMethod("random", paramTypes).invoke(null, paramValues);
            this.entropy = new String(result);
         }
         catch (Throwable t)
         {
            // Fall back on our hash code
            this.entropy = this.toString();
         }
      }

      return (this.entropy);

   }

   /**
    * Set the entropy increaser value.
    *
    * @param entropy The new entropy increaser value
    */
   public void setEntropy(String entropy)
   {
      this.entropy = entropy;
   }

   public String getRandomClass()
   {
      return randomClass;
   }

   public void setRandomClass(String randomClass)
   {
      this.randomClass = randomClass;
   }

   public String getRandomFile()
   {
      return randomFile;
   }

   public void setRandomFile(String randomFile)
   {
      this.randomFile = randomFile;
   }
   
   // ----------------------------------------------------------------  Public
   
   public synchronized String getSessionId()
   {
      return generateSessionId();
   } 

   // -------------------------------------------------------------  Protected
   
   /**
    * Generate a session-id that is not guessable
    *
    * @return generated session-id
    */
   protected synchronized String generateSessionId()
   {
      byte[] bytes = getRandomBytes();

      // Hash the random bytes
      bytes = getDigest().digest(bytes);

      // Render the result as a String of hexadecimal digits
      return encode(bytes);
   }
   
   protected byte[] getRandomBytes()
   {
      byte[] bytes = new byte[SESSION_ID_BYTES];
      
      // Try to read from OS source, e.g. /dev/urandom
      InputStream randomIS = getRandomInputStream();
      if (randomIS != null)
      {
         try 
         {
            if (randomIS.read(bytes) == bytes.length) 
            {
                return bytes;
            }
            log.debug("Failed to read " + bytes.length + 
                      " bytes from random source; closing stream");
         } 
         catch (Exception ex) 
         {
            // Ignore
         }
        
         closeRandomInputStream();
         
      }
      
      // Fall back on a java.util.Random
      getRandom().nextBytes(bytes);
      
      return bytes;
   }

   /**
    * Encode the bytes into a String with a slightly modified Base64-algorithm
    * This code was written by Kevin Kelley <kelley@ruralnet.net>
    * and adapted by Thomas Peuss <jboss@peuss.de>
    *
    * @param data The bytes you want to encode
    * @return the encoded String
    */
   protected String encode(byte[] data)
   {
      char[] out = new char[((data.length + 2) / 3) * 4];
      char[] alphabet = sessionIdAlphabet;

      //
      // 3 bytes encode to 4 chars.  Output is always an even
      // multiple of 4 characters.
      //
      for (int i = 0, index = 0; i < data.length; i += 3, index += 4)
      {
         boolean quad = false;
         boolean trip = false;

         int val = (0xFF & (int) data[i]);
         val <<= 8;
         if ((i + 1) < data.length)
         {
            val |= (0xFF & (int) data[i + 1]);
            trip = true;
         }
         val <<= 8;
         if ((i + 2) < data.length)
         {
            val |= (0xFF & (int) data[i + 2]);
            quad = true;
         }
         out[index + 3] = alphabet[(quad ? (val & 0x3F) : 64)];
         val >>= 6;
         out[index + 2] = alphabet[(trip ? (val & 0x3F) : 64)];
         val >>= 6;
         out[index + 1] = alphabet[val & 0x3F];
         val >>= 6;
         out[index + 0] = alphabet[val & 0x3F];
      }
      return new String(out);
   }

   /**
    * get a random-number generator
    *
    * @return a random-number generator
    */
   protected synchronized Random getRandom()
   {
      if (this.random == null)
      {
         Random r = null;

         // Mix up the seed a bit
         long seed = System.nanoTime();
         char entropy[] = getEntropy().toCharArray();
         for (int i = 0; i < entropy.length; i++)
         {
            long update = ((byte) entropy[i]) << ((i % 8) * 8);
            seed ^= update;
         }

         try
         {
            Class<?> clazz = Class.forName(randomClass);
            r = (Random) clazz.newInstance();
         }
         catch (Exception e)
         {
            log.warn("Exception initializing random number generator of class " + randomClass, e);
            r = new java.util.Random();
         }

         // set the generated seed for this PRNG
         r.setSeed(seed);
         this.random = r;
      }
      return this.random;
   }

   /**
    * get a MessageDigest hash-generator
    *
    * @return a hash generator
    */
   protected synchronized MessageDigest getDigest()
   {
      if (this.digest == null)
      {
         MessageDigest d = null;
         try
         {
            d = MessageDigest.getInstance(algorithm);
         }
         catch (NoSuchAlgorithmException e)
         {
            log.error("MessageDigest algorithm " + algorithm + " is unavailable", e);
            if (SESSION_ID_HASH_ALGORITHM.equals(algorithm) == false)
            {
               try
               {
                  d = MessageDigest.getInstance(SESSION_ID_HASH_ALGORITHM);
               }
               catch (NoSuchAlgorithmException f)
               {
                  log.error("MessageDigest algorithm " + SESSION_ID_HASH_ALGORITHM + " is unavailable", e);
               }
            }
         }

         this.digest = d;
      }

      return this.digest;
   }
   
   // ---------------------------------------------------------------  Private
   
   private InputStream getRandomInputStream()
   {
      if (this.randomIS == null && this.randomFile != null)
      {
         if (Globals.IS_SECURITY_ENABLED)
         {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {

               public Object run()
               {
                  openRandomInputStream();
                  return null;
               }
               
            });
         }
         else 
         {
            openRandomInputStream();
         }
      }
      return this.randomIS;
   }
   
   private void openRandomInputStream()
   {
      try
      {
         File f = new File(this.randomFile);
         if (f.exists())
         {
            this.randomIS = new DataInputStream(new FileInputStream(f));
            this.randomIS.readLong();
            if (log.isDebugEnabled())
            {
               log.debug("Opened " + this.randomFile);
            }
         }
      }
      catch (IOException ex)
      {
         log.warn("Error reading " + this.randomFile, ex);
         closeRandomInputStream();
      }
   }
   
   private void closeRandomInputStream()
   {
      this.randomFile = null;

      if (this.randomIS != null)
      {
         try
         {
            this.randomIS.close();
         }
         catch (Exception e)
         {
            log.warn("Failed to close randomIS.");
         }

         this.randomIS = null;
      }
   }

}
