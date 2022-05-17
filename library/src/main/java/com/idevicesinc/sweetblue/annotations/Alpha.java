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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to tag certain classes or methods that are considered "alpha" level quality.
 * This means that the feature will be release quality at some point in the future, but it
 * may require bug fixes, further documentation, code additions, backwards incompatible changes, moving to different
 * packages, etc., etc., to get to that point, so just be aware.
 */
@Retention(RetentionPolicy.CLASS)
public @interface Alpha
{
}
