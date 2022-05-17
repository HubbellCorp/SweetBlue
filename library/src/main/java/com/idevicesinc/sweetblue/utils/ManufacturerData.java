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

package com.idevicesinc.sweetblue.utils;


/**
 * POJO to hold the manufacturer id, and manufacturer data parsed from a device's scan record.
 */
public class ManufacturerData
{

    /**
     * The manufacturer id
     */
    public short m_id;

    /**
     * Manufacturer data for the manufacturer id stored in {@link #m_id}
     */
    public byte[] m_data;

}
