/**
 *
 * Copyright 2022 Hubbell Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.idevicesinc.sweetblue.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Utilities for dealing with time with an emphasis on BLE.
 */
public final class Utils_Time extends com.idevicesinc.sweetblue.utils.Utils
{
	private Utils_Time(){super();}

	/**
	 * {@link FutureData} wrapper of {@link #getCurrentTime()}.
	 */
	public static FutureData getFutureTime()
	{
		return Utils_Time::getCurrentTime;
	}

	/**
	 * Returns the current time as a byte array, useful for implementing {@link BleServices#currentTime()} for example.
	 */
	public static byte[] getCurrentTime()
	{
		final byte[] time = new byte[10];
		final byte adjustReason = 0;
		GregorianCalendar timestamp = new GregorianCalendar();

		short year = (short) timestamp.get( Calendar.YEAR );
		final byte[] year_bytes = Utils_Byte.shortToBytes(year);
		Utils_Byte.reverseBytes(year_bytes);

		System.arraycopy(year_bytes, 0, time, 0, 2);

		time[2] = (byte)(timestamp.get( Calendar.MONTH ) + 1);
		time[3] = (byte)timestamp.get( Calendar.DAY_OF_MONTH );
		time[4] = (byte)timestamp.get( Calendar.HOUR_OF_DAY );
		time[5] = (byte)timestamp.get( Calendar.MINUTE );
		time[6] = (byte)timestamp.get( Calendar.SECOND );
		time[7] = (byte)timestamp.get( Calendar.DAY_OF_WEEK );
		time[8] = 0; // 1/256 of a second
		time[9] = adjustReason;

		return time;
	}

	/**
	 * {@link FutureData} wrapper of {@link #getLocalTimeInfo()}.
	 */
	public static FutureData getFutureLocalTimeInfo()
	{
		return Utils_Time::getLocalTimeInfo;
	}

	/**
	 * Returns the local time info as a byte array, useful for implementing {@link BleServices#currentTime()} for example.
	 */
	public static byte[] getLocalTimeInfo()
	{
		final byte[] info = new byte[2];
		final TimeZone timeZone = TimeZone.getDefault();
		int offset = timeZone.getOffset( System.currentTimeMillis() );
		offset /= 1800000; // see CTS spec for why this is like this: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.time_zone.xml
		offset *= 2;
		info[0] = (byte)offset;
		final byte dst;

		if ( timeZone.useDaylightTime() && timeZone.inDaylightTime( new GregorianCalendar().getTime() ) )
		{
			final int savings = timeZone.getDSTSavings();
			dst = (byte)(savings / 900000);
		}
		else
		{
			dst = 0;
		}

		info[1] = dst;

		return info;
	}
}
