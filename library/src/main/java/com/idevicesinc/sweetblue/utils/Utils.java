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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleStatuses;

/**
 * Some static utility methods that are probably not very useful outside the library.
 * See subclasses for more specific groups of utility methods.
 */
public class Utils
{
	protected Utils(){}

	static boolean requiresBonding(BluetoothGattCharacteristic characteristic)
	{
		return false;
	}

	public static boolean isLollipop()
	{
		return isAndroidVersion(Build.VERSION_CODES.LOLLIPOP);
	}

	public static boolean isOreo()
	{
		return isAndroidVersion(Build.VERSION_CODES.O);
	}

	public static boolean isMarshmallow()
	{
		return isAndroidVersion(Build.VERSION_CODES.M);
	}

	public static boolean isNougat()
	{
		return isAndroidVersion(Build.VERSION_CODES.N);
	}

	public static boolean isKitKat() {
		return isAndroidVersion(Build.VERSION_CODES.KITKAT);
	}

	public static boolean isAndroid10()
	{
		return isAndroidVersion(Build.VERSION_CODES.Q);
	}

	public static boolean isAndroid12()
	{
		return isAndroidVersion(Build.VERSION_CODES.S);
	}

	public static boolean isAndroid13()
	{
		return isAndroidVersion(Build.VERSION_CODES.TIRAMISU);
	}

	public static boolean isAndroid14()
	{
		return isAndroidVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
	}

	private static boolean isAndroidVersion(int apiVersion)
    {
        return Build.VERSION.SDK_INT >= apiVersion;
    }

	public static boolean isLocationEnabledForScanning_byManifestPermissions(final Context context)
	{
		if( Utils.isMarshmallow() )
		{
			return
					Utils.hasManifestPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
					Utils.hasManifestPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
		}
		else
		{
			return true;
		}
	}

	public static boolean isLocationEnabledForScanning_byRuntimePermissions(final Context context, boolean requestBackgroundOperation)
	{
		if (Utils.isAndroid10())
		{
			boolean loc = context.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, android.os.Process.myPid(), android.os.Process.myUid())  == PackageManager.PERMISSION_GRANTED;
			// if loc is already granted, we still need to make sure background has been granted, if
			// its requested in the config file
			if (loc &&requestBackgroundOperation)
				loc = context.checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, android.os.Process.myPid(), android.os.Process.myUid())  == PackageManager.PERMISSION_GRANTED;
			return loc;
		}
		else if (Utils.isMarshmallow())
		{
			return
					context.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, android.os.Process.myPid(), android.os.Process.myUid())  == PackageManager.PERMISSION_GRANTED ||
					context.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, android.os.Process.myPid(), android.os.Process.myUid())  == PackageManager.PERMISSION_GRANTED ;
		}
		else
		{
			return true;
		}
	}

	public static boolean areBluetoothPermissionsGranted(final Context context, boolean requestBackgroundOpertation, boolean requestAdvertising)
	{
		if (Utils.isAndroid12()) {
			List<String> permissions = new ArrayList<>();
			permissions.add(Manifest.permission.BLUETOOTH_SCAN);
			permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
			if (requestBackgroundOpertation)
				permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
			if (requestAdvertising)
				permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
			for (String permission : permissions) {
				if (context.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED)
					return false;
			}
			return true;
		}
		return false;
	}

	public static boolean hasManifestPermission(final Context context, final String permission)
	{
		try
		{
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

			if( info.requestedPermissions != null )
			{
				for( String p : info.requestedPermissions )
				{
					if( p.equals(permission) )
					{
						return true;
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Utility method used to refresh the GATT database. This is here as a convenience, you shouldn't need to call it yourself. It is called automatically
	 * by the service discovery task if you have {@link com.idevicesinc.sweetblue.BleDeviceConfig#useGattRefresh} set to <code>true</code>.
	 * This method returns false if the method wasn't able to be invoked properly, or {@link Method#invoke(Object, Object...)} returns <code>false</code>
     */
	public static boolean refreshGatt(BluetoothGatt gatt)
	{
		try
		{
			Boolean result = Utils_Reflection.callBooleanReturnMethod(gatt, "refresh", false);

			if( result == null || !result )
			{
				return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}

	/**
	 * Adapted from http://stackoverflow.com/a/22980843/4248895.
	 */
	public static boolean isLocationEnabledForScanning_byOsServices(Context context)
	{
		if( Utils.isMarshmallow() )
		{
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
			{
				final int locationMode;

				try
				{
					locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
				}
				catch(Settings.SettingNotFoundException e)
				{
					return false;
				}

				return locationMode != Settings.Secure.LOCATION_MODE_OFF;
			}
			else
			{
				final String locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

				return false == TextUtils.isEmpty(locationProviders);
			}
		}
		else
		{
			return true;
		}
	}

	public static boolean isLocationEnabledForScanning(final Context context, boolean requestBackgroundOperation)
	{
		if( false == Utils.isMarshmallow() )
		{
			return true;
		}
		else
		{
			if( false == isLocationEnabledForScanning_byManifestPermissions(context) )
			{
				return false;
			}
			else
			{
				if( false == isLocationEnabledForScanning_byRuntimePermissions(context, requestBackgroundOperation) )
				{
					return false;
				}
				else
				{
					if( isLocationEnabledForScanning_byOsServices(context) )
					{
						return true;
					}
					else
					{
						return false;
					}
				}
			}
		}
	}

	/**
	 * Returns true for certain products, which may have problems managing bonding state
	 * and so this method is used in {@link com.idevicesinc.sweetblue.BleDeviceConfig.DefaultBondFilter}.
	 *
	 * So far this method includes these products:<br></br>
	 * All sony devices<br></br>
	 * Motorola ("ghost", and "victara" products)
	 * Samsung ("degaswifiue" product - Tab 4)
	 * AMobile IOT-500 ("full_amobile2601_wp_l" product)
	 */ 
	public static boolean phoneHasBondingIssues()
	{
		return
//			Utils.isManufacturer("lge")																			||
			Utils.isManufacturer("sony")																		||
			Utils.isManufacturer("motorola") && (Utils.isProduct("ghost") || Utils.isProduct("victara"))		||
			Utils.isManufacturer("samsung") && (Utils.isProduct("degaswifiue"))									||
			Utils.isManufacturer("amobile") && Utils.isProduct("full_amobile2601_wp_l")							;
	}
	
	public static boolean isManufacturer(String manufacturer)
	{
		return Build.MANUFACTURER != null && Build.MANUFACTURER.equalsIgnoreCase(manufacturer);
	}
	
	public static boolean isProduct(String product)
	{
		return Build.PRODUCT != null && Build.PRODUCT.contains(product);
	}

	public static boolean isOnMainThread()
	{
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}

	public static boolean isSuccess(int gattStatus)
	{
		return gattStatus == 0;// || gattStatus == 1;
	}

	public static boolean contains(final Object[] uuids, final Object uuid)
	{
		for( int i = 0; i < uuids.length; i++ )
		{
			final Object ith = uuids[i];

			if( ith.equals(uuid) )
			{
				return true;
			}
		}

		return false;
	}

	public static boolean haveMatchingName(String nameNative, List<String> lookedForNames)
	{
		if (TextUtils.isEmpty(nameNative))
			return false;

		if (lookedForNames != null && !lookedForNames.isEmpty())
		{
			String nameNative_lower = nameNative.toLowerCase(Locale.US);

			boolean match = false;

			for (int i = 0; i < lookedForNames.size(); i++)
			{
				if (nameNative_lower.contains(lookedForNames.get(i).toLowerCase(Locale.US)))
				{
					match = true;
					break;
				}
			}

			if (!match)
				return false;
		}

		return true;
	}

	public static boolean haveMatchingIds(List<UUID> advertisedIds, Collection<UUID> lookedForIds)
	{
		if(lookedForIds != null && !lookedForIds.isEmpty())
		{
			boolean match = false;

			for(int i = 0; i < advertisedIds.size(); i++)
			{
				if(lookedForIds.contains(advertisedIds.get(i)))
				{
					match = true;
					break;
				}
			}

			if(!match)
				return false;
		}

		return true;
	}

	public static boolean hasPermission(Context context, String permission)
	{
		int res = context.checkCallingOrSelfPermission(permission);
		
	    return (res == PackageManager.PERMISSION_GRANTED);
	}

	public static <T extends Object> boolean doForEach_break(final Object forEach, final List<T> list)
	{
		final int size = list.size();

		for( int i = 0; i < size; i++ )
		{
			final T ith = list.get(i);

			if( doForEach_break(forEach, ith) )
			{
				return true;
			}
		}

		return false;
	}

	public static boolean doForEach_break(final Object forEach, final Object next)
	{
		if( forEach instanceof ForEach_Void )
		{
			((ForEach_Void)forEach).next(next);
		}
		else if( forEach instanceof ForEach_Breakable )
		{
			final ForEach_Breakable.Please please = ((ForEach_Breakable)forEach).next(next);

			if( false == please.shouldContinue() )
			{
				return true;
			}
		}

		return false;
	}
}
