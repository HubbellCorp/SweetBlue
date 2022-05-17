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

import java.util.UUID;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.TaskTimeoutRequestFilter;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;


abstract class PA_Task implements Comparable<PA_Task>
{
	interface I_StateListener
	{
		void onStateChange(PA_Task task, PE_TaskState state);
	}

	private static final int ORDINAL_NOT_YET_ASSIGNED = -1;
	
	private static final TaskTimeoutRequestFilter.TaskTimeoutRequestEvent s_timeoutRequestEvent = new TaskTimeoutRequestFilter.TaskTimeoutRequestEvent();

	
	private IBleDevice m_device;
	private IBleServer m_server;
	private final IBleManager m_manager;
	
	private double m_timeout;
	private double m_executionDelay = 0.0;
	
	private long m_resetableExecuteStartTime = 0;
	private double m_totalTimeArmedAndExecuting = 0.0;
	
	private final I_StateListener m_stateListener;
	
	private PE_TaskState m_state = null;
	
	private P_TaskManager m_queue;

	private long m_timeCreated;
	private long m_timeExecuted;
	
	private boolean m_softlyCancelled = false;
	
	private int m_defaultOrdinal = ORDINAL_NOT_YET_ASSIGNED; // until added to the queue and assigned an actual ordinal.


    public PA_Task(IBleServer server, I_StateListener listener)
    {
        this(server.getIManager(), listener);

        m_server = server;
    }

	public PA_Task(IBleDevice device, I_StateListener listener)
	{
		this(device.getIManager(), listener);
		
		m_device = device;
	}
	
	public PA_Task(IBleManager manager, I_StateListener listener)
	{
		m_device = null;
		m_manager = manager;
		m_timeCreated = System.currentTimeMillis();
		
		if( listener == null && this instanceof I_StateListener )
		{
			//--- DRK > Can't pass this pointer from subclass up through super(), otherwise that would be cleaner.
			m_stateListener = (I_StateListener) this;
		}
		else
		{
			m_stateListener = listener;
		}

		setState(PE_TaskState.CREATED);
	}
	
	protected abstract BleTask getTaskType();
	
	protected double getInitialTimeout()
	{
		final BleTask taskType = getTaskType();
		
		if( taskType != null )
		{
			final IBleDevice device = getDevice() != null ? getDevice() : P_BleDeviceImpl.NULL;
			final IBleServer server = getServer() != null ? getServer() : P_BleServerImpl.NULL;
			P_Bridge_User.initTaskTimeoutRequestEvent(s_timeoutRequestEvent, BleManager.get(m_manager.getApplicationContext()), getManager().getBleDevice(device), getManager().getBleServer(server), taskType, getCharUuid(), getDescUuid());

			return BleNodeConfig.getTimeout(s_timeoutRequestEvent);
		}
		else
		{
			getManager().ASSERT(false, "BleTask type shouldn't be null.");
			
			return BleNodeConfig.DefaultTaskTimeoutRequestFilter.DEFAULT_TASK_TIMEOUT; // just a back-up, should never be invoked.
		}
	}
	
	protected /*virtual*/ UUID getCharUuid()
	{
		return Uuids.INVALID;
	}
	
	protected /*virtual*/ UUID getDescUuid()
	{
		return Uuids.INVALID;
	}
	
	void init()
	{
		setState(PE_TaskState.INITIALIZED);
	}

	protected P_Logger getLogger()
	{
		return getManager().getLogger();
	}

	/**
	 * Returns <code>true</code> if {@link P_TaskManager#print()} was called.
     */
	private boolean setState(PE_TaskState newState)
	{
		boolean printed = false;
		if( !m_manager.ASSERT(newState != m_state, "") )  return false;
		
		m_state = newState;
		
		if( getLogger().isEnabled() )
		{
			if( m_state.isEndingState() )
			{
				String logText = this.toString();
				if( m_queue != null )
				{
					logText += " - " + m_queue.getUpdateCount();
				}

				getLogger().i(logText);
			}
			else if (m_state == PE_TaskState.EXECUTING )
			{
				getQueue().print();
				printed = true;
			}
		}

		invokeListeners();

		return printed;
	}

	private void invokeListeners()
	{
		if (m_stateListener != null)
			m_stateListener.onStateChange(this, m_state);

		I_StateListener listener = m_manager.getDefaultTaskStateListener();
		if (listener != null)
			listener.onStateChange(this, m_state);
	}

	PE_TaskState getState()
	{
		return m_state;
	}
	
	int getOrdinal()
	{
		return m_defaultOrdinal;
	}

	void assignDefaultOrdinal(final P_TaskManager queue)
	{
		m_defaultOrdinal = m_defaultOrdinal == ORDINAL_NOT_YET_ASSIGNED ? queue.assignOrdinal() : m_defaultOrdinal;
	}
	
	void onAddedToQueue(P_TaskManager queue)
	{
		m_queue = queue;
		setState(PE_TaskState.QUEUED);
//		m_retryCount = 0;
	}
	
	void resetTimeout(double newTimeout)
	{
		//--- DRK > Can be called upstream from different thread than the update loop,
		//---		so preventing clashes here with this.update method.
		m_timeout = newTimeout;
		m_resetableExecuteStartTime = System.currentTimeMillis();
	}
	
	protected void timeout()
	{
		m_queue.tryEndingTask(this, PE_TaskState.TIMED_OUT);
	}
	
	protected void redundant()
	{
		m_queue.tryEndingTask(this, PE_TaskState.REDUNDANT);
	}
	
	protected void succeed()
	{
		m_queue.tryEndingTask(this, PE_TaskState.SUCCEEDED);
	}
	
	protected void fail()
	{
		m_queue.tryEndingTask(this, PE_TaskState.FAILED);
	}

	protected void clearFromQueue()
	{
		if( this.getState() == PE_TaskState.ARMED || this.getState() == PE_TaskState.EXECUTING )
		{
			getManager().ASSERT(false, "Tried to clear a task from queue while its armed or executing.");
		}
		else
		{
			m_queue.tryEndingTask(this, PE_TaskState.CLEARED_FROM_QUEUE);
		}
	}
	
	protected void failImmediately()
	{
		m_queue.tryEndingTask(this, PE_TaskState.FAILED_IMMEDIATELY);
	}
	
	protected void selfInterrupt()
	{
		boolean wasExecuting = this.getState() == PE_TaskState.EXECUTING || this.getState() == PE_TaskState.ARMED;
		
		if( wasExecuting )
		{
			m_queue.tryEndingTask(this, PE_TaskState.INTERRUPTED);
			
			m_queue.add(this);
		}
	}
	
	protected void softlyCancel()
	{
//		m_maxRetries = 0;
		m_queue.tryEndingTask(this, PE_TaskState.SOFTLY_CANCELLED);
	}
	
	protected void failWithoutRetry()
	{
//		m_maxRetries = 0;
		fail();
	}

	void arm()
	{
		setState(PE_TaskState.ARMED);

//		m_totalTimeQueuedAndArmedAndExecuting = m_queue.getTime() - m_addedToQueueTime;
		m_totalTimeArmedAndExecuting = 0.0;
//		m_totalTimeExecuting = 0.0;
		m_resetableExecuteStartTime = System.currentTimeMillis();
//		m_retryCount = 0;
		m_timeout = getInitialTimeout();
	}
	
	protected boolean isExecutable()
	{
		return true;
	}
	
	protected boolean isArmable()
	{
		return true;
	}
	
	private void execute_wrapper()
	{
		m_resetableExecuteStartTime = System.currentTimeMillis();
		m_timeExecuted = System.currentTimeMillis();
		
		execute();
	}
	
	abstract void execute();
	
	void setEndingState(PE_TaskState endingState)
	{
		if( m_softlyCancelled )
		{
			endingState = PE_TaskState.SOFTLY_CANCELLED;
		}
		
		if( !m_manager.ASSERT(endingState.isEndingState(), "") )  return;
		
		//--- DRK > Might be true for timeouts...overall just being defensive but not assertion-level defense.
		if( m_state == endingState )  return;
				
		if( !m_manager.ASSERT(!m_state.isEndingState(), "") )  return;

		setState(endingState);
	}


	/**
	 * Returns <code>true</code> if {@link P_TaskManager#print()} ends up getting called.
 	 */
	public boolean tryExecuting()
	{
		if( m_state == PE_TaskState.ARMED )
		{
			//--- DRK > Force at least one time step between becoming armed and executing.
			//---		TODO: Possibly gate this with an optional time requirement.
			//---				For example we might want to give the ble stack time to "settle" after
			//---				a heavy operation...yes i know how ridiculous that sounds...
//			if( m_updateCount > 1 )
			{
				//--- DRK > Debug code to delay reads and writes and such.
//					if( m_device != null && m_device.is(E_DeviceState.INITIALIZED) )
//					{
//						if( m_totalTimeArmedAndExecuting < 2.0 )
//						{
//							return;
//						}
//					}

				if( m_softlyCancelled )
				{
					softlyCancel();

					return false;
				}

				if( isExecutable() )
				{
					boolean printCalled = setState(PE_TaskState.EXECUTING);

					execute_wrapper();

					return printCalled;
				}
				else
				{
					onNotExecutable();
				}
			}
		}
		return false;
	}
	
	void update_internal(double timeStep, long currentTime)
	{
		m_totalTimeArmedAndExecuting += timeStep;
//			m_totalTimeQueuedAndArmedAndExecuting += timeStep;

		if( m_totalTimeArmedAndExecuting >= m_executionDelay )
		{
			if( m_state == PE_TaskState.ARMED )
			{
				tryExecuting();
			}
			else if( m_state == PE_TaskState.EXECUTING )
			{
				if( !Interval.isDisabled(m_timeout) && m_timeout != Interval.INFINITE.secs() )
				{
					double timeExecuting = (currentTime - m_resetableExecuteStartTime)/1000.0;

					if( timeExecuting >= m_timeout )
					{
						timeout();

						return;
					}
				}
			}
		}

		this.update(timeStep);
	}

	protected void onNotExecutable()
	{
		failWithoutRetry();
	}
	
	protected void update(double timeStep){}
	
	public double getTotalTimeExecuting()
	{
		return (System.currentTimeMillis() - m_timeExecuted)/1000.0;
	}

	public double getTotalTimeExecuting(long currentTime)
	{
		return (currentTime - m_timeExecuted)/1000.0;
	}
	
	public double getTotalTime()
	{
		return (System.currentTimeMillis() - m_timeCreated)/1000.0;
	}

	public double getTotalTime(long currentTime)
	{
		return (currentTime - m_timeCreated)/1000.0;
	}

	public double getAggregatedTimeArmedAndExecuting()
	{
		return m_totalTimeArmedAndExecuting;
	}

	public boolean isFor(final Class<? extends PA_Task_ConnectOrDisconnectServer> taskClass, final IBleServer server, final String macAddress)
	{
		if( taskClass.isAssignableFrom(this.getClass()) )
		{
			final PA_Task_ConnectOrDisconnectServer this_cast = (PA_Task_ConnectOrDisconnectServer) this;

			return this_cast.isFor(server, macAddress);
		}
		else
		{
			return false;
		}
	}
	
	public IBleDevice getDevice()
	{
		return m_device;
	}
	
	public IBleServer getServer()
	{
		return m_server;
	}
	
	public IBleManager getManager()
	{
		return m_manager;
	}
	
	public double getTimeout()
	{
		return m_timeout;
	}
	
	protected P_TaskManager getQueue()
	{
		return m_queue;
	}
	
	public abstract PE_TaskPriority getPriority();
	
	public boolean isMoreImportantThan(PA_Task task)
	{
		return isMoreImportantThan_default(task);
	}
	
	/**
	 * Default implementation to call by subsubclasses if they want to skip their immediate parent's implementation.
	 */
	protected boolean isMoreImportantThan_default(PA_Task task)
	{
		return this.getPriority().ordinal() > task.getPriority().ordinal();
	}
	
	public boolean isInterruptableBy(PA_Task task)
	{
		return false;
	}
	
	public boolean isCancellableBy(PA_Task task)
	{
		return false;
	}
	
	protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		return false;
	}
	
	protected void attemptToSoftlyCancel(PA_Task task)
	{
		m_softlyCancelled = true;
	}

	public boolean wasSoftlyCancelled()
	{
		return m_softlyCancelled;
	}
	
	protected String getToStringAddition()
	{
		return null;
	}
	
	@Override public String toString()
	{
		String name = this.getClass().getSimpleName();
		name = name.replace("P_Task_", "");
		
		String deviceEntry = getDevice() != null ? " " + getDevice().getName_debug(): "";
		String addition = getToStringAddition() != null ? " " + getToStringAddition() : "";
		return name + "(" + (m_state != null ? m_state.name() : "<null>") + deviceEntry + addition + " " + hashCode() + " )";
	}
	
	public boolean isExplicit()
	{
		return false;
	}

	// Utilize the isMoreImportantThan call to decide how these tasks should be ordered
	public int compareTo(PA_Task t)
	{
		if (isMoreImportantThan(t))
			return 1;
		if (t.isMoreImportantThan(this))
			return -1;
		return 0;
	}
}
