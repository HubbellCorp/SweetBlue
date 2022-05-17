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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public final class P_TaskQueue
{
    private List<PA_Task> m_taskList = new ArrayList<>();
    private List<PA_Task> m_lockedList = null;
    private IBleManager m_manager;
    private Object m_lock = new Object();

    public P_TaskQueue(IBleManager manager)
    {
        m_manager = manager;
    }

    static class HandlerResult
    {
        PA_Task mTask;
        int mTaskPosition;

        private HandlerResult(PA_Task task, int taskPosition)
        {
            mTask = task;
            mTaskPosition = taskPosition;
        }

        PA_Task getTask()
        {
            return mTask;
        }

        int getTaskPosition()
        {
            return mTaskPosition;
        }
    }

    abstract static class ForEachTaskHandler
    {
        enum ProcessResult
        {
            Continue,
            ContinueAndDequeue,
            Return,
            ReturnAndDequeue
        };
        public abstract ProcessResult process(PA_Task task);
    }

    /**
     * Allows for forward iteration of the task queue. You <b>MUST</b> remember to syncrhonize to {@link P_TaskManager#m_lock} when calling this
     * method.
     */
    final HandlerResult forEachTask(ForEachTaskHandler handler)
    {
        synchronized (m_lock)
        {
            boolean createdLockedList = false;
            if (m_lockedList == null)
            {
                m_lockedList = new ArrayList<>(m_taskList);
                createdLockedList = true;
            }

            try
            {
                Iterator<PA_Task> it = m_lockedList.iterator();
                int index = 0;

                while (it.hasNext())
                {
                    PA_Task task = it.next();

                    // Avoid iterating over this task if it is no longer in the list
                    // This allows us to 'see' removes w/o actually modifying the locked list
                    if (!m_taskList.contains(task))
                        continue;

                    ForEachTaskHandler.ProcessResult pr = handler.process(task);

                    switch (pr)
                    {
                        case Return:
                            return new HandlerResult(task, index);

                        case ReturnAndDequeue:
                            // We can directly remove the task now, as the method is returning right after this.
                            m_taskList.remove(task);
                            return new HandlerResult(task, index);

                        case ContinueAndDequeue:
                            // As we are iterating over a clone, we simply add the removed tasks to a new list to be removed later.
                            m_taskList.remove(task);
                            //removeList.add(task);
                            break;
                    }
                    index++;
                }

                return new HandlerResult(null, -1);
            }
            finally
            {
                // Discard the locked list (if we created it)
                if (createdLockedList)
                    m_lockedList = null;
            }
        }
    }

    final PA_Task peek()
    {
        //FIXME:  Should this use the locked list?
        synchronized (m_lock)
        {
            return m_taskList.size() > 0 ? m_taskList.get(0) : null;
        }
    }

    // This is only used for printing the task queue (apparently)
    final PA_Task get(int index)
    {
        synchronized (m_lock)
        {
            return m_taskList.get(index);
        }
    }

    final void pushFront(PA_Task task)
    {
        //m_manager.ASSERT(!m_inForEach, "Tried to push to the front when in a foreach iteration!");
        synchronized (m_lock)
        {
            m_taskList.add(0, task);
        }
    }

    final void pushBack(PA_Task task)
    {
        //m_manager.ASSERT(!m_inForEach, "Tried to push to the back when in a foreach iteration!");
        synchronized (m_lock)
        {
            m_taskList.add(m_taskList.size(), task);
        }
    }

    final void insertAtSoonestPosition(PA_Task task)
    {
        synchronized (m_lock)
        {
            //m_manager.ASSERT(!m_inForEach, "Tried to insert at soonest position when in a foreach iteration!");
            // Before walking the entire list, see if the task is more important than the last.  If not, just throw it in the back
            if (m_taskList.size() > 0)
            {
                PA_Task last = m_taskList.get(m_taskList.size() - 1);
                if (!task.isMoreImportantThan(last))
                {
                    m_taskList.add(task);
                    return;
                }
            }

            //FIXME:  This can be done with a modified binary search that finds the first element that task claims it is more important than

            // Locate the best spot to insert, and put the task there
            for (int i = 0; i < m_taskList.size(); ++i)
            {
                if (task.isMoreImportantThan(m_taskList.get(i)))
                {
                    m_taskList.add(i, task);
                    return;
                }
            }

            m_taskList.add(task);
        }
    }

    final int size()
    {
        synchronized (m_lock)
        {
            return m_taskList.size();
        }
    }

    final List<PA_Task> getRaw()
    {
        synchronized (m_lock)
        {
            return new ArrayList<>(m_taskList);
        }
    }

    @Override public final String toString()
    {
        return m_taskList.toString();
    }
}
