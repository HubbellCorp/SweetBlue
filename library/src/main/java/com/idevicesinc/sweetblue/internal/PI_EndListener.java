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

package com.idevicesinc.sweetblue.internal;

import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.BleTransaction.EndReason;
import com.idevicesinc.sweetblue.ReadWriteListener;

interface PI_EndListener
{
	void onTransactionEnd(IBleTransaction txn, EndReason reason, ReadWriteListener.ReadWriteEvent failReason);
}