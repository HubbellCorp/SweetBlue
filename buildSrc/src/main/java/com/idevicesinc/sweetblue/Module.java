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

package com.idevicesinc.sweetblue;

/**
 * Enumeration used to dictate the release-able modules in the library. The enum name should be exactly what the
 * artifact id is for maven (eg sweetblue, sweetbluerx, etc). The argument for each enum value also contains the
 * actual directory name for said module (eg library, rx). When pushing a release, the build system goes through each
 * value in this enum via the values() method.
 */
public enum Module
{
    sweetblue("library"),
    sweetbluerx("rx"),
    sweetunit("sweetunit");


    private String mDirName;


    Module(String dirName)
    {
        mDirName = dirName;
    }


    public String getDirName()
    {
        return mDirName;
    }
}
