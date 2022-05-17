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

import com.idevicesinc.sweetblue.utils.Utils_Byte;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;


final class PU_File
{
	private static String EXTENSION = "history";
	private static String FILENAME_DELIMITER = "_";

	static byte readByte(final FileInputStream in) throws IOException
	{
		byte toReturn = 0;

		{
			toReturn = (byte) in.read();
		}

		return toReturn;
	}

	static short readShort(final FileInputStream in, final byte[] tempBuffer) throws IOException
	{
		short toReturn = 0;

		{
			in.read(tempBuffer, 0, 2);
			toReturn = Utils_Byte.bytesToShort(tempBuffer);
		}

		return toReturn;
	}

	static int readInt(final FileInputStream in, final byte[] tempBuffer) throws IOException
	{
		int toReturn = 0;

		{
			in.read(tempBuffer, 0, 4);
			toReturn = Utils_Byte.bytesToInt(tempBuffer);
		}

		return toReturn;
	}

	static long readLong(final FileInputStream in, final byte[] tempBuffer) throws IOException
	{
		long toReturn = 0;

		{
			in.read(tempBuffer, 0, 8);
			toReturn = Utils_Byte.bytesToLong(tempBuffer);
		}

		return toReturn;
	}

	static String makeHistoryFileName(final String macAddress, final UUID uuid)
	{
		return macAddress + FILENAME_DELIMITER + uuid.toString() + "." + EXTENSION;
	}
}
