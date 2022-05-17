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
 */
enum PE_TaskPriority
{
	TRIVIAL,	// for now only for scanning.
	LOW,		// for normal reads and writes and such.
	MEDIUM,		// default level for connection and bonding related tasks.
	HIGH,		// for implicit bonding and connection events, for example if user did something through another app or the OS, or came back into range.
	CRITICAL;	// for enabling/disabling ble and for removing bonds and disconnecting before actual ble disable.
	
	static final PE_TaskPriority FOR_NORMAL_READS_WRITES				= LOW;
	static final PE_TaskPriority FOR_EXPLICIT_BONDING_AND_CONNECTING	= MEDIUM;
	static final PE_TaskPriority FOR_PRIORITY_READS_WRITES				= MEDIUM;
	static final PE_TaskPriority FOR_IMPLICIT_BONDING_AND_CONNECTING	= HIGH;
}
