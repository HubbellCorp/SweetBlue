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

import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.P_NativeGattObject;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.UUID;


/**
 * Wrapper class which holds an instance of {@link BluetoothGattDescriptor}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattDescriptor} returned from {@link #getDescriptor()}.
 */
public final class BleDescriptor extends P_NativeGattObject<BluetoothGattDescriptor>
{


    private BleDescriptor()
    {
        super(null, null);
    }

    public BleDescriptor(BluetoothGattDescriptor descriptor)
    {
        super(descriptor);
    }

    public BleDescriptor(UhOhListener.UhOh uhOh)
    {
        super(uhOh);
    }

    BleDescriptor(BluetoothGattDescriptor descriptor, UhOhListener.UhOh uhoh)
    {
        super(descriptor, uhoh);
    }

    /**
     * Returns the instance of {@link BluetoothGattDescriptor} held in this class.
     */
    public final BluetoothGattDescriptor getDescriptor()
    {
        return getGattObject();
    }

    /**
     * Returns the {@link UUID} of this descriptor. If the descriptor is <code>null</code>, then {@link Uuids#INVALID} is returned.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) UUID getUuid()
    {
        return getDescriptor() != null ? getDescriptor().getUuid() : Uuids.INVALID;
    }

    /**
     * Returns this descriptor's current value. If the descriptor is <code>null</code>, an empty byte array will be returned.
     */
    public final byte[] getValue()
    {
        return getDescriptor() != null ? getDescriptor().getValue() : P_Const.EMPTY_BYTE_ARRAY;
    }

    final boolean setValue(byte[] value)
    {
        return getDescriptor() != null && getDescriptor().setValue(value);
    }

    /**
     * Returns this descriptor's parent characteristic. If the descriptor is <code>null</code> {@link BleCharacteristic#NULL} will be returned.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) BleCharacteristic getCharacteristic()
    {
        return getDescriptor() != null ? new BleCharacteristic(getDescriptor().getCharacteristic()) : BleCharacteristic.NULL;
    }

    public final static BleDescriptor NULL = new BleDescriptor();

}
