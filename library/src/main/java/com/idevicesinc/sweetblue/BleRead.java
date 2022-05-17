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


import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.UUID;

/**
 * Builder-type class used when performing reads on BLE devices.
 */
public class BleRead extends BleOp<BleRead>
{

    /**
     * "Invalid" static instance used when reading things like RSSI, or setting connection parameters
     */
    public final static BleRead INVALID = new BleRead(Uuids.INVALID, Uuids.INVALID);


    public BleRead()
    {
    }

    public BleRead(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleRead(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }

    /**
     * Constructor which creates a new {@link BleRead} from the one given. This will only copy over the service, characteristic, and descriptor Uuids. It will
     * NOT copy over any listeners, or filters.
     */
    public BleRead(BleRead read)
    {
        super(read.getServiceUuid(), read.getCharacteristicUuid());
    }

    @Override
    public final boolean isValid()
    {
        return getCharacteristicUuid() != null;
    }

    @Override
    final BleRead createDuplicate()
    {
        return getDuplicateOp();
    }

    @Override
    final BleRead createNewOp()
    {
        return new BleRead();
    }



    /**
     * Builder class to build out a list (or array) of {@link BleRead} instances.
     */
    public final static class Builder extends BleOp.Builder<Builder, BleRead>
    {

        public Builder()
        {
            this(null, null);
        }

        public Builder(UUID characteristicUuid)
        {
            this(null, characteristicUuid);
        }

        public Builder(UUID serviceUuid, UUID characteristicUuid)
        {
            currentOp = new BleRead(serviceUuid, characteristicUuid);
        }

        @Override
        public final BleRead[] buildArray()
        {
            return build().toArray(new BleRead[0]);
        }
    }
}
