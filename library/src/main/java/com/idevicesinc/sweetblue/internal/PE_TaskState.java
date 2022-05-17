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

/**
 * 
 * 
 *
 */
enum PE_TaskState
{
	CREATED,				// very transient, not really listened for at the moment.
	INITIALIZED,			// another transient state, indicates it has been initialized (it's init() method was called)
	QUEUED,					// task is in queue...previous state can be CREATED or INTERRUPTED
	
	ARMED,					// task is current but not yet executing...there's a configurable time for being in this state but usually ends up just being one timeStep. 
	EXECUTING,
	
	// ending states
	SUCCEEDED,
	TIMED_OUT,
	INTERRUPTED,			// put back on queue...next state will be QUEUED.
	CANCELLED,
	SOFTLY_CANCELLED,		// set after arming (preemptively cancels execution) or is mutated from the SUCCEEDED state if task is softly cancelled while already executing.
	FAILED,
	CLEARED_FROM_QUEUE,
	REDUNDANT,
	FAILED_IMMEDIATELY;		// same as FAILED but to indicate that operation couldn't even be sent off, presumably due to very exceptional conditions.
	
	public boolean isEndingState()
	{
		return this.ordinal() > EXECUTING.ordinal();
	}

	public boolean canGoToNextTaskImmediately()
	{
		return this == SUCCEEDED || this == TIMED_OUT;
	}
}
	
