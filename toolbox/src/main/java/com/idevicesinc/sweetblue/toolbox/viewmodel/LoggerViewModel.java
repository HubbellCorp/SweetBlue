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

package com.idevicesinc.sweetblue.toolbox.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.idevicesinc.sweetblue.toolbox.util.AppConfig;
import com.idevicesinc.sweetblue.toolbox.util.DebugLog;
import com.idevicesinc.sweetblue.toolbox.util.MutablePostLiveData;
import com.idevicesinc.sweetblue.utils.DebugLogger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoggerViewModel extends ViewModel implements DebugLogger.LogEvent
{

    private MutablePostLiveData<StringBuilder> m_logString;
    private MutablePostLiveData<String> m_filter;
    private LogLevel m_logLevel = LogLevel.Verbose;


    public enum LogLevel
    {
        Unused,
        Unused2,
        Verbose,
        Debug,
        Info,
        Warn,
        Error,
        Assert,
        Unknown
    };


    public LoggerViewModel()
    {
        m_logString = new MutablePostLiveData<>();
        m_logString.setValue(new StringBuilder());
        m_filter = new MutablePostLiveData<>();
        m_filter.setValue("");
        DebugLog.getDebugger().setLogListener(this);

        try
        {
            String logLevelString = AppConfig.getInstance().getConfigurationOption(AppConfig.ConfigurationOption.LogLevel);
            LogLevel logLevel = logLevelString != null ? LogLevel.valueOf(logLevelString) : null;
            if (logLevel != null)
                m_logLevel = logLevel;
        }
        catch (Exception e)
        {
        }
    }

    public LiveData<StringBuilder> getLogStringData()
    {
        return m_logString;
    }

    public void setLogLevel(LogLevel level)
    {
        m_logLevel = level;

        try
        {
            AppConfig.getInstance().setConfigurationOption(AppConfig.ConfigurationOption.LogLevel, m_logLevel.name());
        }
        catch (Exception e)
        {

        }
    }

    public LogLevel getLogLevel()
    {
        return m_logLevel;
    }

    public void refreshLog()
    {

        final String filter = m_filter.getValue() != null ? m_filter.getValue() : "";

        final StringBuilder logString = new StringBuilder();
        List<String> log = DebugLog.getDebugger().getLogList();
        Pattern filterPattern = Pattern.compile("(?i)" + filter);

        for (int i = 0; i < log.size(); i++)
        {
            String entry = log.get(i).substring(11, 20) + log.get(i).substring(29);
            Matcher entryMatcher = filterPattern.matcher(entry);
            LogLevel entryLevel  = extractLogLevel(entry);

            if (m_logLevel == LogLevel.Verbose || entryLevel.ordinal() >= m_logLevel.ordinal())
            {
                if (filter.equals("") || entryMatcher.find())
                {
                    logString.append(entry + "\n\n");
                }
            }
        }
        m_logString.setValue(logString);
    }

    public void setFilter(final String filter)
    {
        m_filter.setValue(filter);
    }

    @Override
    public void onLogEntry(String entry)
    {
        writeNewLine(entry);
    }

    private LogLevel extractLogLevel(String logMessage)
    {
        // Log level is printed directly before the first slash
        try
        {
            int slashIndex = logMessage.indexOf('/');
            int spaceIndex = logMessage.lastIndexOf(' ', slashIndex);
            String level = logMessage.substring(spaceIndex + 1, slashIndex);

            LogLevel ll = LogLevel.valueOf(level);
            if (ll != null)
                return ll;
        }
        catch (Exception e)
        {
        }
        return LogLevel.Unknown;

    }

    private void writeNewLine(String entry)
    {

        final String filter = m_filter.getValue() != null ? m_filter.getValue() : "";

        Pattern filterPattern = Pattern.compile(filter);

        final String newEntry = entry.substring(11, 20) + entry.substring(29);
        Matcher entryMatcher = filterPattern.matcher(newEntry);
        LogLevel entryLevel  = extractLogLevel(newEntry);
        if (m_logLevel == LogLevel.Verbose || entryLevel.ordinal() >= m_logLevel.ordinal())
        {
            if (filter.equals("") || entryMatcher.find())
            {
                final StringBuilder builder = m_logString.getValue();
                builder.append(newEntry).append("\n\n");
                m_logString.setValue(builder);
            }
        }
    }

    @Override
    protected void onCleared()
    {
        DebugLog.getDebugger().setLogListener(null);
    }
}
