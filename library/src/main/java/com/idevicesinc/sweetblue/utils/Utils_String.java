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

import android.annotation.SuppressLint;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Utility methods for string manipulation and creation needed by SweetBlue, mostly for debug purposes.
 */
public class Utils_String extends Utils
{
	private Utils_String(){super();}

	private static final int FRACTION_DIGITS = 2;

	private static final DecimalFormat s_toFixedFormat = new DecimalFormat();
	{
		s_toFixedFormat.setMaximumFractionDigits(FRACTION_DIGITS);
		s_toFixedFormat.setMinimumFractionDigits(FRACTION_DIGITS);
	}

	public static String toFixed(final double value)
	{
		return s_toFixedFormat.format(value);
	}

	public static String bytesToMacAddress(final byte[] raw)
	{
		return String.format("%02X:%02X:%02X:%02X:%02X:%02X", raw[0],raw[1],raw[2],raw[3],raw[4],raw[5]);
	}

	public static String normalizeMacAddress(final String macAddress)
	{
		return normalizeMacAddress_replaceDelimiters(macAddress.toUpperCase(Locale.US));
	}

	public static String normalizeMacAddress_replaceDelimiters(final String macAddress)
	{
		final char[] commonDelimiters = {'-', '.', ' ', '_'};

		if( macAddress == null )
		{
			return "";
		}
		else if( macAddress.length() == 0 )
		{
			return "";
		}
		else
		{
			for( int i = 0; i < commonDelimiters.length; i++ )
			{
				final String commonDelimiter_ith = String.valueOf(commonDelimiters[i]);

				if( macAddress.contains(commonDelimiter_ith) )
				{
					return macAddress.replace(commonDelimiter_ith, ":");
				}
			}
		}

		return macAddress;
	}

	public static String normalizeDeviceName(String deviceName)
	{
		if( deviceName == null || deviceName.length() == 0 )  return "";

		String[] nameParts = deviceName.split("-");
		// Customer reported having a device with the name of "----", which would result in an empty array
		// Best to just return an empty string here.
		if (nameParts.length == 0) return "";
		String consistentName = nameParts[0];
		consistentName = consistentName.toLowerCase(Locale.US);
		consistentName = consistentName.trim();
		consistentName = consistentName.replace(" ", "_");

		return consistentName;
	}

	public static String debugizeDeviceName(String macAddress, String normalizedName, boolean isNativeDeviceNull)
	{
		String[] address_split = macAddress.split(":");
		StringBuilder b = new StringBuilder();
		b.append(normalizedName.length() == 0 ? "<no_name>" : normalizedName);
		if (!isNativeDeviceNull)
		{
			b.append("_").append(address_split[address_split.length - 2]).append(address_split[address_split.length - 1]);
		}
		return b.toString();
	}

	public static String getStringValue(final byte[] data, final String charset)
	{
		String string = "";
		byte[] value = data;

		if(value != null && value.length > 0)
		{
			try
			{
				string = new String(value, charset);
			}
			catch(UnsupportedEncodingException e)
			{
				return "";
			}

			string = string.trim();
		}

		return string;
	}

	public static String getStringValue(final byte[] data)
	{
		return getStringValue(data, "UTF-8");
	}

	private static class FlagOnStyle extends CharacterStyle
	{
		@Override public void updateDrawState(TextPaint tp)
		{
			tp.setColor(0xFF006400);
		}
	};

	private static class FlagOffStyle extends CharacterStyle
	{
		@Override public void updateDrawState(TextPaint tp)
		{
			tp.setColor(0xFFFF0000);
			tp.setStrikeThruText(true);
		}
	};

	public static SpannableString makeStateString(State[] states, int stateMask)
	{
		String rawString = "";
		String spacer = "  ";

		for(int i = 0; i < states.length; i++)
		{
			if( states[i].isNull() )  continue;

			String name = ((Enum) states[i]).name();
			rawString += name + spacer;
		}

		SpannableString spannableString = new SpannableString(rawString);

		int position = 0;
		for(int i = 0; i < states.length; i++)
		{
			if( states[i].isNull() )  continue;

			String name = ((Enum) states[i]).name();

			if(states[i].overlaps(stateMask))
			{
				spannableString.setSpan(new FlagOnStyle(), position, position + name.length(), 0x0);
			}
			else
			{
				spannableString.setSpan(new FlagOffStyle(), position, position + name.length(), 0x0);
			}

			position += name.length() + spacer.length();
		}

		return spannableString;
	}

	public static String concatStrings(String... strings)
	{
		StringBuilder b = new StringBuilder();
		for (String s : strings)
		{
			b.append(s);
		}
		return b.toString();
	}

	public static String makeString(Object... objects)
	{
		StringBuilder builder = new StringBuilder();
		if (objects != null)
		{
			for (Object o : objects)
			{
				builder.append(o);
			}
		}
		return builder.toString();
	}

	public static String prettyFormatLogList(List<String> logEntries)
	{
		int size = logEntries == null ? 0 : logEntries.size();
		StringBuilder b = new StringBuilder();
		b.append("Log entry count: ").append(size);
		if (size > 0)
		{
			b.append("\n\n");
			b.append("Entries:\n\n[\n");
			for (int i = 0; i < size; i++)
			{
				b.append(logEntries.get(i)).append("\n\n");
			}
			b.append("]\n");
		}
		return b.toString();
	}

	public static String toString(int mask, State[] values)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[");

		boolean foundFirst = false;

		for( int i = 0; i < values.length; i++ )
		{
			if( values[i].overlaps(mask) )
			{
				if( foundFirst )
				{
					builder.append(", ");
				}
				else
				{
					foundFirst = true;
				}

				builder.append(values[i]);
			}
		}

		builder.append("]");

		return builder.toString();
	}

	public static String toString(Class<?> type, Object ... values)
	{
		StringBuilder builder = new StringBuilder();

		builder.append(type.getSimpleName());

		int length_highest = 0;
		for( int i = 0; i < values.length; i+=2 )
		{
			int length_ith = values[i].toString().length();

			if( length_ith > length_highest )
			{
				length_highest = length_ith;
			}
		}

		for( int i = 0; i < values.length; i+=2 )
		{
			builder.append("\n   ");

			final int length_ith = values[i].toString().length();
			final int spaceCount = length_highest - length_ith;

			builder.append(values[i]);

			for( int j = 0; j < spaceCount; j++ )
			{
				builder.append(" ");
			}
			builder.append(" = ");
			builder.append(values[i+1]);
		}

		return builder.toString();
	}

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public enum HexOption
	{
		/**
		 * Tells the {@link #hexStringToBytes(String, HexOption...)}  method that it should accept hex strings
		 * in lower case.
		 */
		ALLOW_LOWERCASE,

		/**
		 * Tells {@link #hexStringToBytes(String, HexOption...)} that it should allow spaces in the string.
		 */
		ALLOW_SPACES,

		/**
		 * Tells {@link #hexStringToBytes(String, HexOption...)} that it should allow the standard hex prefix of 0x, which will
		 * just get stripped out.
		 */
		ALLOW_HEX_PREFIX,

		/**
		 * Tells {@link #hexStringToBytes(String, HexOption...)} that it should allow odd count hex strings, and to prepend a 0 to make
		 * them a compliant hex string.
		 */
		ADD_PRECEDING_ZERO
	}


	/**
	 * Convert a hex string into a byte array. This method can be fairly robust, depending on the
	 * {@link HexOption}s you pass in. If a HexOption isn't specified when a certain condition happens,
	 * <code>null</code> will be returned. For instance, if there are spaces in the string, and you
	 * <b>don't</b> pass in {@link HexOption#ALLOW_SPACES}, <code>null</code> will be returned.
	 */
	@SuppressLint("squid:S1157")
	public static byte[] hexStringToBytes(String string, HexOption... options)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		List<HexOption> optionList = (options != null ? new ArrayList<>(Arrays.asList(options)) : new ArrayList<HexOption>());

		// Ensure that case isn't an issue. If it IS, and it contains lower case chars, then bail out
		if (optionList.contains(HexOption.ALLOW_LOWERCASE))
			string = string.toUpperCase(Locale.US).trim();
		else if (!string.equals(string.toUpperCase(Locale.US)))
			return null;

		boolean allowPrefix = optionList.contains(HexOption.ALLOW_HEX_PREFIX);
		boolean addZero = optionList.contains(HexOption.ADD_PRECEDING_ZERO);

		// If we don't allow the hex prefix, check for it and early out if it's there
		if (!allowPrefix && (string.contains("0X") || string.contains("0x"))) return null;

		if (!string.contains(" "))
		{
			boolean odd = string.length() % 2 != 0;

			if (!addZero && odd) return null;

			if (allowPrefix) string = string.replace("0X", "");

			if (addZero && odd) string = "0" + string;

			for (int idx = 0; idx + 2 <= string.length(); idx += 2)
			{
				String hexStr = string.substring(idx, idx + 2);
				int intValue = Integer.parseInt(hexStr, 16);
				baos.write(intValue);
			}
		}
		else
		{
			if (optionList.contains(HexOption.ALLOW_SPACES))
			{
				String[] splits = string.split(" ");
				for (String s : splits)
				{
					byte[] section = hexStringToBytes(s, options);

					if (section == null)
						return null;

					for (byte b : section)
						baos.write(b);
				}
			}
			else
				return null;
		}

		return baos.toByteArray();
	}

	/**
	 * Overload of {@link #hexStringToBytes(String, HexOption...)}, which passes in every {@link HexOption}.
	 */
	public static byte[] hexStringToBytes(String string)
	{
		return hexStringToBytes(string, HexOption.values());
	}

	/**
	 * Convert a byte array to a hex string. The hex strings created by this method will always return
	 * capitalized letters.
	 */
	public static String bytesToHexString(final byte[] bytes)
	{
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Convert a byte array to a binary string
	 */
	public static String bytesToBinaryString(final byte[] bytes)
	{
		return bytesToBinaryString(bytes, -1);
	}

	/**
	 * Convert a byte array to a binary string
	 * Also inserts spaces between every count bytes (for legibility)
	 */
	public static String bytesToBinaryString(final byte[] bytes, int bytesBetweenSpaces)
	{
		if (bytes == null)
			return null;

		StringBuilder sb = new StringBuilder();

		int count = 0;

		for (byte b : bytes)
		{
			for (int i = 0; i < 4; ++i)
			{
				int downShift = (3 - i) * 2;
				int quarterByte = (b>>downShift) & 0x3;
				switch (quarterByte)
				{
					case 0x0:
						sb.append("00");
						break;
					case 0x1:
						sb.append("01");
						break;
					case 0x2:
						sb.append("10");
						break;
					case 0x3:
						sb.append("11");
						break;
				}
			}

			if (++count >= bytesBetweenSpaces && bytesBetweenSpaces != -1)
				sb.append(' ');
		}

		return sb.toString().trim();
	}

	/**
	 * Converts a binary string to a byte array
	 */
	public static byte[] binaryStringToBytes(String s) throws Exception
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		// Iterate over the string and make a byte array
		int b = 0;
		int count = 0;
		for (int index = s.length() - 1; index >= 0; index--)
		{
			char c = s.charAt(index);

			if (c != '0' && c != '1')
			{
				// Whitespace is simply ignored
				if (Character.isWhitespace(c))
					continue;

				// Anything else is an error
				//FIXME:  Custom exception type
				throw new Exception("Illegal character " + c + " encountered while parsing binary string");
			}

			int val = c == '1' ? 1 : 0;
			b >>>= 1;
			b |= (val<<7);

			if (((++count) % 8) == 0)
			{
				os.write(b);
				b = 0;
			}
		}

		// Final byte (if needed)
		int extraBytes = (count % 8);
		if (extraBytes > 0)
		{
			b >>>= (8-extraBytes);
			os.write(b);
		}

		// Make, then reverse the output array
		byte output[] = os.toByteArray();
		for (int i = 0; i < output.length / 2; ++i)
		{
			byte temp = output[i];
			int swapIndex = output.length - 1 - i;
			output[i] = output[swapIndex];
			output[swapIndex] = temp;
		}

		return output;
	}

	/**
	 * Returns <code>true</code> if the given {@link String} is null, or has a length of 0.
	 */
	public static boolean isEmpty(String string)
	{
		return string == null || string.length() == 0;
	}
}
