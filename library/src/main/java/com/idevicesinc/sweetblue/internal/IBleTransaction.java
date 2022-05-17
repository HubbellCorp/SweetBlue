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


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleTransaction;

/**
 * Interface to define contract for methods in a transaction.
 */
public interface IBleTransaction extends IBleTransaction_User, IBleTransaction_Internal
{
    /**
     * Interface to define contract for callback methods for a transaction.
     */
    interface Callback
    {
        void updateTxn(double timeStep);
        void startTxn(BleDevice device);
        void onEndTxn(BleDevice device, BleTransaction.EndReason reason);
        BleTransaction.Atomicity getAtomicity();
    }

    /**
     * Factory interface used to instantiate a new internal transaction class
     */
    interface Factory
    {
        IBleTransaction newInstance(Callback callback);
    }

    Factory DEFAULT_FACTORY = new DefaultFactory();

    /**
     * Default class to create a new instance of IBleTransaction (implemented via BleTransactionImpl)
     */
    class DefaultFactory implements Factory
    {
        @Override
        public IBleTransaction newInstance(Callback callback)
        {
            return new BleTransactionImpl(callback);
        }
    }
}
