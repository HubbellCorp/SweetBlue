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
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Provide an implementation to {@link BleManager#reset(ResetListener)}
 * to be notified when a reset operation is complete.
 *
 * @see BleManager#reset(ResetListener)
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface ResetListener extends GenericListener_Void<ResetListener.ResetEvent>
{

    /**
     * Enumeration of the progress of the reset.
     * More entries may be added in the future.
     */
    enum Progress
    {
        /**
         * The reset has completed successfully.
         */
        COMPLETED;
    }

    /**
     * Struct passed to {@link ResetListener#onEvent(ResetListener.ResetEvent)}.
     */
    @Immutable
    class ResetEvent extends Event
    {
        /**
         * The {@link BleManager} the reset was applied to.
         */
        public BleManager manager(){  return m_manager;  }
        private final BleManager m_manager;

        /**
         * The progress of the reset.
         */
        public ResetListener.Progress progress(){  return m_progress;  }
        private final ResetListener.Progress m_progress;

        ResetEvent(BleManager manager, ResetListener.Progress progress)
        {
            m_manager = manager;
            m_progress = progress;
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "progress",		progress()
                    );
        }
    }

    /**
     * The reset event, for now only fired when the reset is completed. Hopefully the bluetooth stack is OK now.
     */
    void onEvent(final ResetListener.ResetEvent e);

}
