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


public abstract class BleDescriptorOp<T extends BleDescriptorOp> extends BleOp<T>
{

    UUID m_descriptorUuid;


    public BleDescriptorOp()
    {
    }

    public BleDescriptorOp(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleDescriptorOp(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid)
    {
        super(serviceUuid, characteristicUuid);
        m_descriptorUuid = descriptorUuid;
    }

    public BleDescriptorOp(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }

    /**
     * Constructor which creates a new {@link BleDescriptorOp} from the one given. This will only copy over the service, characteristic, and descriptor Uuids. It will
     * NOT copy over any listeners, or filters.
     */
    public BleDescriptorOp(BleDescriptorOp descOp)
    {
        super(descOp.getServiceUuid(), descOp.getCharacteristicUuid());
        setDescriptorUUID(descOp.getDescriptorUuid());
    }



    /**
     * Set the descriptor UUID (if operating with a descriptor).
     */
    public final T setDescriptorUUID(UUID uuid)
    {
        m_descriptorUuid = uuid;
        return (T) this;
    }

    @Override public boolean isValid()
    {
        return Uuids.isValid(getCharacteristicUuid()) && Uuids.isValid(getDescriptorUuid());
    }

    @Override T getDuplicateOp()
    {
        BleDescriptorOp op = super.getDuplicateOp();
        op.setDescriptorUUID(m_descriptorUuid);
        return (T) op;
    }

    static abstract class Builder<B extends BleOp.Builder, T extends BleDescriptorOp> extends BleOp.Builder<B, T>
    {


        /**
         * Set the descriptor UUID (if operating with a descriptor).
         */
        public final B setDescriptorUUID(UUID uuid)
        {

            currentOp.setDescriptorUUID(uuid);
            return (B) this;
        }
    }

}
