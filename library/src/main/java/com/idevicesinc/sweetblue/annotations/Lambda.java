/*

  Copyright 2022 Hubbell Incorporated

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to denote <code>interface</code> declarations whose implementations can essentially act like lambdas (i.e. anonymous functions).
 * Implementations are technically classes and not language-level lambda constructs because
 * Java at this time does not support them. Conceptually however they can be treated as lambdas.
 *
 * As of Android Studio 3.0, Java 8 and lambdas are supported. This annotation will remain to mark interfaces which should remain
 * a lambda (only one method), so this annotation becomes more for internal use than anything else.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Lambda
{
}
