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


import java.util.UUID;


public class BleDescriptorRead extends BleDescriptorOp<BleDescriptorRead>
{


    public BleDescriptorRead()
    {
    }

    public BleDescriptorRead(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleDescriptorRead(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid)
    {
        super(serviceUuid, characteristicUuid, descriptorUuid);
    }

    public BleDescriptorRead(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }

    /**
     * Constructor which creates a new {@link BleDescriptorRead} from the one given. This will only copy over the service, characteristic, and descriptor Uuids. It will
     * NOT copy over any listeners, or filters.
     */
    public BleDescriptorRead(BleDescriptorRead read)
    {
        super(read.getServiceUuid(), read.getCharacteristicUuid(), read.getDescriptorUuid());
    }


    @Override BleDescriptorRead createDuplicate()
    {
        return getDuplicateOp();
    }

    @Override BleDescriptorRead createNewOp()
    {
        return new BleDescriptorRead();
    }


    /**
     * Builder class to build out a list (or array) of {@link BleDescriptorRead} instances.
     */
    public final static class Builder extends BleDescriptorOp.Builder<Builder, BleDescriptorRead>
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
            currentOp = new BleDescriptorRead(serviceUuid, characteristicUuid);
        }

        public Builder(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid)
        {
            currentOp = new BleDescriptorRead(serviceUuid, characteristicUuid, descriptorUuid);
        }

        @Override
        public final BleDescriptorRead[] buildArray()
        {
            return build().toArray(new BleDescriptorRead[0]);
        }
    }
}
