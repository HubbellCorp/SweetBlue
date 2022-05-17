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

package com.idevicesinc.sweetblue.utils;

import android.util.Log;

import com.idevicesinc.sweetblue.TimeTrackerSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeTracker
{
    private static TimeTracker s_instance = null;

    public static void createInstance(TimeTrackerSetting tts)
    {
        s_instance = new TimeTracker(tts);
    }

    public static TimeTracker getInstance()
    {
        /*if (s_instance == null)
            s_instance = new TimeTracker();*/
        return s_instance;
    }

    public class Record implements Comparable<Record>
    {
        // Record name, also key in map
        private String m_tag;

        // Total time accumulated
        private long m_timeAccumulator;

        // How many times this record has been updated
        private long m_hitCount;

        // Longest single update
        private long m_longestTime;

        // Shortest single update
        private long m_shortestTime;

        Record(StackEntry se, long timeNow)
        {
            m_tag = se.getTag();

            assert(m_tag != null);

            m_timeAccumulator = timeNow - se.getStartTime();

            m_hitCount = 1;

            m_longestTime = m_timeAccumulator;

            m_shortestTime = m_timeAccumulator;
        }

        void update(StackEntry se, long timeNow)
        {
            assert(m_tag.equals(se.getTag()));

            long dt = timeNow - se.getStartTime();

            m_timeAccumulator += dt;

            ++m_hitCount;

            m_longestTime = Math.max(m_longestTime, dt);

            m_shortestTime = Math.min(m_shortestTime, dt);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("TimeTracker Record for '" + m_tag + "' - (");
            sb.append("hits: " + m_hitCount + ", ");
            sb.append("total: " + printTimeNanos(m_timeAccumulator) + ", ");
            sb.append("avg: " + printTimeNanos(m_timeAccumulator / m_hitCount) + ", ");
            sb.append("best: " + printTimeNanos(m_shortestTime) + ", ");
            sb.append("worst: " + printTimeNanos(m_longestTime) + ")");

            return sb.toString();
        }

        private String printTimeNanos(long timeNanos)
        {
            long timeMs = timeNanos / 1000000;
            if (timeMs > 0)
                return "" + timeMs + "ms";

            long timeFrac = timeNanos / 1000;
            if (timeFrac > 0)
                return "" + (timeFrac / 1000.0f) + "ms";

            return "0ms";
        }

        @Override
        public int compareTo(Record record)
        {
            return m_tag.compareTo(record.m_tag);
        }
    }

    private class StackEntry
    {
        private String m_tag;
        private long m_startTime;

        StackEntry(String tag, long startTime)
        {
            m_tag = tag;
            m_startTime = startTime;
        }

        String getTag()
        {
            return m_tag;
        }

        long getStartTime()
        {
            return m_startTime;
        }
    }

    private TimeTrackerSetting m_timeTrackerSetting;

    private Object m_lock = new Object();

    private List<StackEntry> m_stack = new ArrayList<>();
    private Map<String, Record> m_records = new HashMap<>();

    private boolean checkAbort(boolean printing)
    {
        if (printing)
            return m_timeTrackerSetting != TimeTrackerSetting.RecordAndPrint;
        return m_timeTrackerSetting != TimeTrackerSetting.RecordOnly && m_timeTrackerSetting != TimeTrackerSetting.RecordAndPrint;
    }

    public TimeTracker(TimeTrackerSetting tts)
    {
        m_timeTrackerSetting = tts;
    }

    public void start(String tag)
    {
        if (checkAbort(false))
            return;
        start(tag, System.nanoTime());
    }

    public void start(String tag, long timeNow)
    {
        if (checkAbort(false))
            return;

        if (tag == null)
            tag = "";

        synchronized (m_lock)
        {
            m_stack.add(new StackEntry(tag, timeNow));
        }
    }

    public void stop(String tag)
    {
        if (checkAbort(false))
            return;
        stop(tag, System.nanoTime());
    }

    private void stop(String tag, long timeNow)
    {
        if (checkAbort(false))
            return;

        if (tag == null)
            tag = "";

        synchronized (m_lock)
        {
            if (m_stack.size() < 1)
            {
                assert (false);
                return;
            }

            int idx = m_stack.size() - 1;
            StackEntry se = m_stack.get(idx);
            m_stack.remove(idx);

            // Make sure the tag matches the previous entry...  This is just a redundancy check
            if (!se.getTag().equals(tag))
            {
                assert (false);
                return;
            }

            Record r = m_records.get(tag);
            if (r == null)
            {
                r = new Record(se, timeNow);
                m_records.put(tag, r);
            }
            else
                r.update(se, timeNow);
        }
    }

    public void transition(String stopTag, String startTag)
    {
        if (checkAbort(false))
            return;
        long timeNow = System.nanoTime();
        stop(stopTag, timeNow);
        start(startTag, timeNow);
    }

    public void clear()
    {
        if (checkAbort(false))
            return;
        synchronized (m_lock)
        {
            m_records.clear();
        }
    }

    public void clear(String tag)
    {
        if (checkAbort(false))
            return;

        if (tag == null)
            tag = "";

        synchronized (m_lock)
        {
            m_records.remove(tag);
        }
    }

    public void print()
    {
        if (checkAbort(true))
            return;
        synchronized (m_lock)
        {
            List<Record> records = new ArrayList<>(m_records.values());
            Collections.sort(records);
            Log.i("TimeTracker", "Sorted Results:");
            for (Record r : records)
                Log.i("TimeTracker", r.toString());
        }
    }

    public void setTimeTrackerSetting(TimeTrackerSetting tts)
    {
        m_timeTrackerSetting = tts;
    }
}
