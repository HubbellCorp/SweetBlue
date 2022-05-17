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


import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Pointer;
import java.util.List;
import java.util.UUID;


final class P_ServerServiceManager extends PA_ServiceManager
{
	private final IBleServer m_server;

	private AddServiceListener m_listener = null;

	P_ServerServiceManager(final IBleServer server)
	{
		m_server = server;
	}

	public final void setListener(AddServiceListener listener)
	{
		m_listener = listener;
	}

	@Override public final BleService getServiceDirectlyFromNativeNode(final UUID uuid)
	{
		final IBluetoothServer server_native = m_server.getNativeLayer();

		if( server_native.isServerNull() )
		{
			return BleService.NULL;
		}
		else
		{
			final BleService service = server_native.getService(uuid);

			return service;
		}
	}

	@Override protected final List<BleService> getNativeServiceList_original()
	{
		final IBluetoothServer server_native = m_server.getNativeLayer();

		if( server_native.isServerNull() )
		{
			return P_Const.EMPTY_BLESERVICE_LIST;
		}
		else
		{
			final List<BleService> list_native = server_native.getServices();

			return list_native;
		}
	}

	private void getTasks(ForEach_Breakable<P_Task_AddService> forEach)
	{
		final P_TaskManager queue = m_server.getIManager().getTaskManager();
		final List<PA_Task> queue_raw = queue.getRaw();

		for( int i = queue_raw.size()-1; i >= 0; i-- )
		{
			final PA_Task ith = queue_raw.get(i);

			if( ith.getClass() == P_Task_AddService.class && m_server.equals(ith.getServer()) )
			{
				final P_Task_AddService task_cast = (P_Task_AddService) ith;

				final ForEach_Breakable.Please please = forEach.next(task_cast);

				if( please.shouldBreak() )
				{
					return;
				}
			}
		}

		final PA_Task current = queue.getCurrent();

		if( current != null )
		{
			if( current.getClass() == P_Task_AddService.class && m_server.equals(current.getServer()) )
			{
				final P_Task_AddService current_cast = (P_Task_AddService) current;

				if( !current_cast.cancelledInTheMiddleOfExecuting() )
				{
					forEach.next(current_cast);
				}
			}
		}
	}

	private boolean alreadyAddingOrAdded(final BleService serviceToBeAdded)
	{
		final BleService existingServiceFromServer = getServiceDirectlyFromNativeNode(serviceToBeAdded.getUuid());

		if( !existingServiceFromServer.isNull() && equals(existingServiceFromServer, serviceToBeAdded) )
		{
			return true;
		}
		else
		{
			final Pointer<Boolean> mutableBool = new Pointer<>(false);

			getTasks(next -> {
                final BleService service_ith = next.getService();

                if (equals(service_ith, serviceToBeAdded))
                {
                    mutableBool.value = true;

                    return ForEach_Breakable.Please.doBreak();
                }
                else
                {
                    return ForEach_Breakable.Please.doContinue();
                }
            });

			return mutableBool.value;
		}
	}

	public final AddServiceListener.ServiceAddEvent addService(final BleService service, final AddServiceListener listener_specific_nullable)
	{
		if( m_server.isNull() )
		{
			final AddServiceListener.ServiceAddEvent e = P_Bridge_User.newServiceAddEvent_EARLY_OUT(m_server.getBleServer(), service.getService(), AddServiceListener.Status.NULL_SERVER);

			invokeListeners(e, listener_specific_nullable);

			return e;
		}
		else if( false == m_server.getIManager().is(BleManagerState.ON) )
		{
			final AddServiceListener.ServiceAddEvent e = P_Bridge_User.newServiceAddEvent_EARLY_OUT(m_server.getBleServer(), service.getService(), AddServiceListener.Status.BLE_NOT_ON);

			invokeListeners(e, listener_specific_nullable);

			return e;
		}
		else if( alreadyAddingOrAdded(service) )
		{
			final AddServiceListener.ServiceAddEvent e = P_Bridge_User.newServiceAddEvent_EARLY_OUT(m_server.getBleServer(), service.getService(), AddServiceListener.Status.DUPLICATE_SERVICE);

			invokeListeners(e, listener_specific_nullable);

			return e;
		}
		else
		{
			final P_Task_AddService task = new P_Task_AddService(m_server, service, listener_specific_nullable);
			m_server.getIManager().getTaskManager().add(task);

			return P_Bridge_User.newServiceAddEvent_NULL(m_server.getBleServer(), service.getService());
		}
	}

	public final void removeAll(final AddServiceListener.Status status)
	{
		final IBluetoothServer server_native = m_server.getNativeLayer();

		if( !server_native.isServerNull() )
		{
			server_native.clearServices();
		}

		getTasks(next -> {
            next.cancel(status);

            return ForEach_Breakable.Please.doContinue();
        });
	}

	public final BleService remove(final UUID serviceUuid)
	{
		final BleService service = getServiceDirectlyFromNativeNode(serviceUuid);

		if( service.isNull() )
		{
			final Pointer<BleService> pointer = new Pointer<>();

			getTasks(next -> {
                if (next.getService().getUuid().equals(serviceUuid))
                {
                    pointer.value = next.getService();

                    next.cancel(AddServiceListener.Status.CANCELLED_FROM_REMOVAL);

                    return ForEach_Breakable.Please.doBreak();
                }
                else
                {
                    return ForEach_Breakable.Please.doContinue();
                }
            });

			return pointer.value;
		}
		else
		{
			final IBluetoothServer server_native = m_server.getNativeLayer();

			if( server_native.isServerNull() )
			{
				m_server.getIManager().ASSERT(false, "Didn't expect native server to be null when removing characteristic.");

				return null;
			}
			else
			{
				server_native.removeService(service);

				return service;
			}
		}
	}

	public final void invokeListeners(final AddServiceListener.ServiceAddEvent e, final AddServiceListener listener_specific_nullable)
	{
		AddServiceListener listener = listener_specific_nullable;
		if( listener != null )
			m_server.getIManager().postEvent(listener, e);

		listener = m_listener;
		if( listener != null )
			m_server.getIManager().postEvent(listener, e);

		listener = m_server.getIManager().getDefaultAddServiceListener();
		if( listener != null )
			m_server.getIManager().postEvent(listener, e);
	}
}
