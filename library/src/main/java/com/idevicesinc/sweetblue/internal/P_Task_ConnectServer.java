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
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;


final class P_Task_ConnectServer extends PA_Task_ConnectOrDisconnectServer
{
	private ServerReconnectFilter.Status m_status = ServerReconnectFilter.Status.NULL;

	public P_Task_ConnectServer(IBleServer server, P_DeviceHolder nativeDevice, I_StateListener listener, boolean explicit, PE_TaskPriority priority)
	{
		super(server, nativeDevice, listener, explicit, priority);
	}

	public ServerReconnectFilter.Status getStatus()
	{
		return m_status;
	}

	@Override public void execute()
	{
		final IBluetoothServer server_native_nullable = getServer().getNativeLayer();

		if( server_native_nullable.isServerNull() )
		{
			if( !getServer().getNativeManager().openServer() )
			{
				m_status = ServerReconnectFilter.Status.SERVER_OPENING_FAILED;

				failImmediately();

				return;
			}
		}

		final IBluetoothServer server_native = getServer().getNativeLayer();

		if( server_native.isServerNull() )
		{
			m_status = ServerReconnectFilter.Status.SERVER_OPENING_FAILED;

			failImmediately();

			getManager().ASSERT(false, "Server should not be null after successfully opening!");
		}
		else
		{
			if( getServer().getNativeManager().isDisconnected(m_nativeDevice.getAddress()) )
			{
				if( server_native.connect(m_nativeDevice, false) )
				{
					// SUCCESS! At least, we will wait and see.
				}
				else
				{
					m_status = ServerReconnectFilter.Status.NATIVE_CONNECTION_FAILED_IMMEDIATELY;

					failImmediately();
				}
			}
			else
			{
				if( getServer().getNativeManager().isDisconnecting(m_nativeDevice.getAddress()) )
				{
					m_status = ServerReconnectFilter.Status.NATIVE_CONNECTION_FAILED_IMMEDIATELY;

					failImmediately();

					getManager().ASSERT(false, "Server is currently disconnecting a client when we're trying to connect.");
				}
				else if( getServer().getNativeManager().isConnecting(m_nativeDevice.getAddress()) )
				{
					//--- DRK > We don't fail out, but this is a good sign that something's amiss upstream.
					getManager().ASSERT(false, "Server is already connecting to the given client.");
				}
				else if( getServer().getNativeManager().isConnected(m_nativeDevice.getAddress()) )
				{
					m_status = ServerReconnectFilter.Status.ALREADY_CONNECTING_OR_CONNECTED;

					redundant();
				}
				else
				{
					m_status = ServerReconnectFilter.Status.NATIVE_CONNECTION_FAILED_IMMEDIATELY;

					failImmediately();

					getManager().ASSERT(false, "Native server state didn't match any expected values.");
				}
			}
		}
	}

	@Override public boolean isCancellableBy(PA_Task task)
	{
		if( task instanceof P_Task_DisconnectServer )
		{
			if( task.getClass() == P_Task_DisconnectServer.class && this.getServer().equals(task.getServer()) )
			{
				final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

				if( task_cast.m_nativeDevice.getAddress().equals(m_nativeDevice.getAddress()) )
				{
					//--- DRK > If an implicit disconnect comes in we have no choice but to bail.
					//---		Otherwise we let the connection task run its course then we'll
					//---		disconnect afterwards all nice and orderly-like.
					if( !task_cast.isExplicit() )
					{
						return true;
					}
				}
			}
		}
		else if( task instanceof P_Task_TurnBleOff )
		{
			return true;
		}

		return super.isCancellableBy(task);
	}

	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_DisconnectServer.class && this.getServer().equals(task.getServer()) )
		{
			final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

			if( task_cast.m_nativeDevice.getAddress().equals(m_nativeDevice.getAddress()) )
			{
				if( this.isExplicit() )
				{
					return true;
				}
			}
		}

		return super.isSoftlyCancellableBy(task);
	}

	public void onNativeFail(final int gattStatus)
	{
		m_gattStatus = gattStatus;

		fail();
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.CONNECT_SERVER;
	}
}
