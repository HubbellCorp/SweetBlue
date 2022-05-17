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


import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Mostly only for SweetBlue library developers. Provide an implementation to
 * {@link BleManager#setListener_Assert(AssertListener)} to be notified whenever
 * an assertion fails through {@link BleManager#ASSERT(boolean, String)}.
 */
@Advanced
@com.idevicesinc.sweetblue.annotations.Lambda
public interface AssertListener extends GenericListener_Void<AssertListener.AssertEvent>
{

    /**
     * Struct passed to {@link AssertListener#onEvent(AssertListener.AssertEvent)}.
     */
    @Immutable
    class AssertEvent extends Event
    {
        /**
         * The {@link BleManager} instance for your application.
         */
        public BleManager manager(){  return m_manager;  }
        private final BleManager m_manager;

        /**
         * Message associated with the assert, or an empty string.
         */
        public String message(){  return m_message;  }
        private final String m_message;

        /**
         * Stack trace leading up to the assert.
         */
        public StackTraceElement[] stackTrace(){  return m_stackTrace;  }
        private final StackTraceElement[] m_stackTrace;

        AssertEvent(BleManager manager, String message, StackTraceElement[] stackTrace)
        {
            m_manager = manager;
            m_message = message;
            m_stackTrace = stackTrace;
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "message",			message(),
                            "stackTrace",		stackTrace()
                    );
        }
    }

    /**
     * Provides additional info about the circumstances surrounding the assert.
     */
    void onEvent(final AssertEvent e);

}
