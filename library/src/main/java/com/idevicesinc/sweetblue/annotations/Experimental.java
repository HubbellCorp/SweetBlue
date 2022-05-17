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
import java.lang.annotation.Target;

/**
 * Classes, or methods marked with this annotation are experimental features. You should be aware that if you use anything marked with this
 * annotation, that the resulting behavior may be unpredictable.
 */
@Target(value = {ElementType.TYPE, ElementType.METHOD})
public @interface Experimental
{
}
