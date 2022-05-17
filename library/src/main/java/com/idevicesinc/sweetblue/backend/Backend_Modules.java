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

package com.idevicesinc.sweetblue.backend;

import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDataList;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDataList_Default;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase_Default;

/**
 * A collection of {@link java.lang.Class} instances used through {@link Class#newInstance()} to create instances of backend modules.
 */
public class Backend_Modules
{
	public static Class<? extends Backend_HistoricalDataList> HISTORICAL_DATA_LIST = Backend_HistoricalDataList_Default.class;
	public static Class<? extends Backend_HistoricalDatabase> HISTORICAL_DATABASE = Backend_HistoricalDatabase_Default.class;
}
