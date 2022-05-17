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

/**
 * Utility methods for byte and bit twiddling.
 */
public final class Utils_Byte
{

	private Utils_Byte(){}

	/**
	 * Converts a var-args of {@link BitwiseEnum}s to an int of their bit values
     */
	public static int toBits(final BitwiseEnum ... enums)
	{
		int bits = 0x0;

		for( int i = 0; i < enums.length; i++ )
		{
			bits |= enums[i].bit();
		}

		return bits;
	}

	/**
	 * Reverses the byte array order. This is useful when dealing with bluetooth hardware that is in a different endianness than android.
	 */
	public static void reverseBytes(byte[] data)
	{
		for( int i = 0; i < data.length/2; i++ )
		{
			byte first = data[i];
			byte last = data[data.length-1-i];

			data[i] = last;
			data[data.length-1-i] = first;
		}
	}

	/**
	 * Convert a boolean to a byte (<code>true</code> is 0x1, <code>false</code> is 0x0).
	 */
	public static byte boolToByte(final boolean value)
	{
		return (byte) (value ? 0x1 : 0x0);
	}

	/**
	 * Convert a byte to a boolean
	 */
	public static boolean byteToBool(byte val)
	{
		return val != 0x0;
	}

	/**
	 * Outputs a {@link Boolean} from the given byte array, starting at the offset provided. If there is not enough space to
	 * create the short, <code>null</code> will be returned.
	 */
	public static Boolean byteToBool(byte[] b, int offset)
	{
		return (offset < 0 || offset >= b.length) ? null : b[offset] != 0x0;
	}

	/**
	 * Convert a short to a byte array
	 */
	public static byte[] shortToBytes(short l)
	{
		byte[] result = new byte[2];
		for( short i = 1; i >= 0; i-- )
		{
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	/**
	 * Convert a byte array to a short
	 */
	public static short bytesToShort(byte[] b)
	{
		short result = 0;
		int loopLimit = Math.min(2, b.length);
		for( short i = 0; i < loopLimit; i++ )
		{
			result <<= 8;
			result |= (b[i] & 0xFF);
		}

		return result;
	}

	/**
	 * Outputs a {@link Short} from the given byte array, starting at the offset provided. If there is not enough space to
	 * create the short, <code>null</code> will be returned.
	 */
	public static Short bytesToShort(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Short.SIZE / 8))
			return null;
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Short.SIZE / 8));
			return bytesToShort(bytes);
		}
	}

	/**
	 * Convert an int to a byte array
	 */
	public static byte[] intToBytes(int l)
	{
		byte[] result = new byte[4];
		for( int i = 3; i >= 0; i-- )
		{
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	/**
	 * Convert a byte array to an int
	 */
	public static int bytesToInt(byte[] b)
	{
		int result = 0;
		int loopLimit = Math.min(4, b.length);
		for( int i = 0; i < loopLimit; i++ )
		{
			result <<= 8;
			result |= (b[i] & 0xFF);
		}

		return result;
	}

	/**
	 * Outputs an {@link Integer} from the given byte array, starting at the offset provided. If there is not enough space to
	 * create the short, <code>null</code> will be returned.
	 */
	public static Integer bytesToInt(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Integer.SIZE / 8))
			return null;
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Integer.SIZE / 8));
			return bytesToInt(bytes);
		}
	}

	/**
	 * Convert a long to a byte array
	 */
	public static byte[] longToBytes(long l)
	{
		byte[] result = new byte[8];
		for( int i = 7; i >= 0; i-- )
		{
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	/**
	 * Convert a byte array to a long
	 */
	public static long bytesToLong(byte[] b)
	{
		long result = 0;
		int loopLimit = Math.min(8, b.length);
		for( int i = 0; i < loopLimit; i++ )
		{
			result <<= 8;
			result |= (b[i] & 0xFF);
		}
		return result;
	}

	/**
	 * Outputs a {@link Long} from the given byte array, starting at the offset provided. If there is not enough space to
	 * create the short, <code>null</code> will be returned.
	 */
	public static Long bytesToLong(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Long.SIZE / 8))
			return null;
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Long.SIZE / 8));
			return bytesToLong(bytes);
		}
	}

	/**
	 * Convert a float to a byte array
	 */
	public static byte[] floatToBytes(float f)
	{
		int intBits = Float.floatToIntBits(f);
		return intToBytes(intBits);
	}

	/**
	 * Convert a byte array to a float
	 */
	public static float bytesToFloat(byte[] b)
	{
		int intBits = bytesToInt(b);
		return Float.intBitsToFloat(intBits);
	}

	/**
	 * Outputs a {@link Float} from the given byte array, starting at the offset provided. If there is not enough space to
	 * create the short, <code>null</code> will be returned.
	 */
	public static Float bytesToFloat(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Float.SIZE / 8))
			return null;
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Float.SIZE / 8));
			return bytesToFloat(bytes);
		}
	}

	/**
	 * Convert a double to a byte array
	 */
	public static byte[] doubleToBytes(double d)
	{
		long longBits = Double.doubleToLongBits(d);
		return longToBytes(longBits);
	}

	/**
	 * Convert a byte array to a double
	 */
	public static double bytesToDouble(byte[] b)
	{
		long longBits = bytesToLong(b);
		return Double.longBitsToDouble(longBits);
	}

	/**
	 * Outputs a {@link Double} from the given byte array, starting at the offset provided. If there is not enough space to
	 * create the short, <code>null</code> will be returned.
	 */
	public static Double bytesToDouble(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Double.SIZE / 8))
			return null;
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Double.SIZE / 8));
			return bytesToDouble(bytes);
		}
	}

	/**
	 * Create a new byte array from the given source, with the given ranges
	 */
	public static byte[] subBytes(byte[] source, int sourceBegin_index_inclusive, int sourceEnd_index_exclusive)
	{
		byte[] destination = new byte[sourceEnd_index_exclusive - sourceBegin_index_inclusive];
		System.arraycopy(source, sourceBegin_index_inclusive, destination, 0, sourceEnd_index_exclusive - sourceBegin_index_inclusive);
		return destination;
	}

	/**
	 * Create a new byte array from the given source, starting at the given begin index
	 */
	public static byte[] subBytes(final byte[] source, final int sourceBegin)
	{
		return subBytes(source, sourceBegin, source.length);
	}

	/**
	 * Returns <code>true</code> if the given byte array is either null, or has a length of 0.
	 */
	public static boolean isEmpty(byte[] bytes)
	{
		return bytes == null || bytes.length == 0;
	}

	/**
	 * Copy from one byte array to another, with the given size, and offsets.
	 * Returns <code>false</code> if the source, or destination arrays are <code>null</code>, size + destOffset is greater than
	 * the destination array length, or size + sourceOffset is greater than the source array length. Otherwise, <code>true</code>
	 * is returned. This would also mean nothing is done to either array.
	 */
	public static boolean memcpy(byte[] dest, byte[] source, int size, int destOffset, int sourceOffset)
	{
		if (dest == null || source == null || (size + destOffset) > dest.length || (size + sourceOffset) > source.length) return false;

		for(int i = 0; i < size; i++)
			dest[i+destOffset] = source[i+sourceOffset];

		return true;
	}

	/**
	 * Overload of {@link #memcpy(byte[], byte[], int, int, int)}, using 0 for both offsets
	 */
	public static boolean memcpy(byte[] dest, byte[] source, int size)
	{
		return memcpy(dest, source, size,0, 0);
	}

	/**
	 * Set a value to size indexes in the given byte array
	 * Returns <code>false</code> if the byte array is null, or the size is greater than the array's length. This means
	 * nothing will happen to the byte array given.
	 */
	public static boolean memset(byte[] data, byte value, int size)
	{
		if (data == null || size > data.length) return false;

		for(int i = 0; i < size; i++)
			data[i] = value;

		return true;
	}

	/**
	 * Set a value to size indexes in the given byte array starting at the given offset.
	 * Returns <code>false</code> if the offset is less than 0, or the array is null, or
	 * the size + the offset are greater than the array's length. The byte array is left alone
	 * in this case.
	 */
	public static boolean memset(byte[] data, byte value, int offset, int size)
	{
		if (data == null || offset < 0 || size + offset > data.length) return false;

		for(int i = offset; i < (size + offset); i++)
			data[i] = value;

		return true;
	}

	/**
	 * Compare two byte arrays. Returns <code>true</code> if each value matches for the given size
	 */
	public static boolean memeq(byte[] buffer1, byte[] buffer2, int size)
	{
		for(int i = 0; i < size; i++)
		{
			if(buffer1[i] != buffer2[i])
				return false;
		}

		return true;
	}

	/**
	 * Compare two byte arrays.
	 */
	public static int memcmp(byte[] b1, byte[] b2, int size)
	{
		for (int i = 0; i < size; i++)
		{
			if (b1[i] != b2[i])
			{
				if ((b1[i] >=0 && b2[i] >= 0) || (b1[i] < 0 && b2[i] < 0))
				{
					return b1[i] - b2[i];
				}
				if (b1[i] < 0 && b2[i] >= 0)
				{
					return 1;
				}
				if (b2[i] < 0 && b1[i] >= 0)
				{
					return -1;
				}
			}
		}
		return 0;
	}
}
