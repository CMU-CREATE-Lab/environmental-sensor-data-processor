/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.util;

import java.util.regex.Pattern;

public class XMLDateFilter implements Filter
   {
   private final static String PATTERN =
         "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}";
   private final Pattern pattern;

   public XMLDateFilter()
      {
      pattern = Pattern.compile(PATTERN);
      }

   public String filter(final String in)
      {
      if (in != null && pattern.matcher(in).find())
         {
         return pattern.matcher(in).replaceAll("yyyy-MM-ddTHH:mm:ss.SSS");
         }
      else
         {
         return in;
         }
      }
   }
