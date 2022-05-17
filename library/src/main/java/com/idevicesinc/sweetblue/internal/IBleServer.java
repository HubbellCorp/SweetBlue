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


import com.idevicesinc.sweetblue.utils.UsesCustomNull;

/**
 * Interface which represents a Bluetooth Server (the android device acts as a bluetooth peripheral)
 */
public interface IBleServer extends IBleNode, IBleServer_User, IBleServer_Internal, UsesCustomNull
{

    /**
     * Factory instance used to instantiate a new instance of IBleServer
     */
    interface Factory
    {
        IBleServer newInstance(final IBleManager mngr, final boolean isNull);
    }

    /**
     * Default factory instance to create new instances of IBleServer via BleServerImpl.
     */
    Factory DEFAULT_FACTORY = new DefaultFactory();

    class DefaultFactory implements Factory
    {
        @Override
        public IBleServer newInstance(IBleManager mngr, boolean isNull)
        {
            return new P_BleServerImpl(mngr, isNull);
        }
    }

}
