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



import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Provide an implementation to {@link BleManager#setListener_State(ManagerStateListener)} to receive callbacks
 * when the {@link BleManager} undergoes a {@link BleManagerState} change.
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface ManagerStateListener extends GenericListener_Void<ManagerStateListener.StateEvent>
{

    /**
     * Subclass that adds the manager field.
     */
    @Immutable
    class StateEvent extends State.ChangeEvent<BleManagerState>
    {
        /**
         * The singleton manager undergoing the state change.
         */
        public BleManager manager(){  return m_manager;  }
        private final BleManager m_manager;

        StateEvent(final BleManager manager, final int oldStateBits, final int newStateBits, final int intentMask)
        {
            super(oldStateBits, newStateBits, intentMask);

            this.m_manager = manager;
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "entered",			Utils_String.toString(enterMask(), BleManagerState.VALUES()),
                            "exited",			Utils_String.toString(exitMask(), BleManagerState.VALUES()),
                            "current",			Utils_String.toString(newStateBits(), BleManagerState.VALUES())
                    );
        }
    }

    @Override
    void onEvent(StateEvent e);
}
