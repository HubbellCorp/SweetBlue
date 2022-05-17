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


import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;


public abstract class P_NativeGattObject<T> implements UsesCustomNull
{

    private final UhOhListener.UhOh m_uhOh;
    private final T m_gattObject;


    public P_NativeGattObject()
    {
        this(null, null);
    }

    public P_NativeGattObject(T gattObject)
    {
        this(gattObject, null);
    }

    public P_NativeGattObject(UhOhListener.UhOh uhOh)
    {
        this(null, uhOh);
    }

    public P_NativeGattObject(T gattObject, UhOhListener.UhOh uhOh)
    {
        m_gattObject = gattObject;
        m_uhOh = uhOh;
    }


    /**
     * Mostly used internally, but if there was a particular issue when retrieving a gatt object, it will have an {@link com.idevicesinc.sweetblue.UhOhListener.UhOh}
     * with a status of what went wrong.
     */
    public UhOhListener.UhOh getUhOh()
    {
        return m_uhOh;
    }

    public boolean hasUhOh()
    {
        return m_uhOh != null;
    }

    /**
     * Returns <code>true</code> if the gatt object held in this class is <code>null</code> or not.
     */
    @Override
    public boolean isNull()
    {
        return m_gattObject == null;
    }



    public T getGattObject()
    {
        return m_gattObject;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (obj instanceof P_NativeGattObject)
        {
            P_NativeGattObject other = (P_NativeGattObject) obj;
            if (other.isNull() && isNull())
                return true;
            if (other.getGattObject().equals(getGattObject()))
                return true;
        }
        return false;
    }
}
