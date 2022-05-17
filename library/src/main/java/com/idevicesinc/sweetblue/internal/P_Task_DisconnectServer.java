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

import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;


final class P_Task_DisconnectServer extends PA_Task_ConnectOrDisconnectServer
{
	public P_Task_DisconnectServer(final IBleServer server, final P_DeviceHolder nativeDevice, final I_StateListener listener, final boolean explicit, PE_TaskPriority priority)
	{
		super( server, nativeDevice, listener, explicit, priority );
	}

	@Override void execute()
	{
		final IBluetoothServer server_native = getServer().getNativeLayer();

		if( server_native.isServerNull() )
		{
			failImmediately();

			getManager().ASSERT(false, "Tried to disconnect client from server but native server is null.");
		}
		else
		{
			if( getServer().getNativeManager().isDisconnected(m_nativeDevice.getAddress()) )
			{
				redundant();
			}
			else if( getServer().getNativeManager().isConnecting(m_nativeDevice.getAddress()) )
			{
				failImmediately();

				getManager().ASSERT(false, "Server is currently connecting a client when we're trying to disconnect.");
			}
			else if( getServer().getNativeManager().isDisconnecting(m_nativeDevice.getAddress()) )
			{
				//--- DRK > We don't fail out, but this is a good sign that something's amiss upstream.
				getManager().ASSERT(false, "Server is already disconnecting from the given client.");
			}
			else if( getServer().getNativeManager().isConnected(m_nativeDevice.getAddress()) )
			{
				server_native.cancelConnection(m_nativeDevice);

				// SUCCESS!
			}
			else
			{
				failImmediately();

				getManager().ASSERT(false, "Native server state didn't match any expected values.");
			}
		}
	}

	public void onNativeSuccess(int gattStatus)
	{
		m_gattStatus = gattStatus;

		succeed();
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.DISCONNECT_SERVER;
	}

	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_ConnectServer.class && this.getServer().equals(task.getServer()) )
		{
			final P_Task_ConnectServer task_cast = (P_Task_ConnectServer) task;

			if( task_cast.m_nativeDevice.getAddress().equals(m_nativeDevice.getAddress()) )
			{
				//if( isCancellable() )
				{
					return true;
				}
			}
		}

		return super.isSoftlyCancellableBy(task);
	}
}
