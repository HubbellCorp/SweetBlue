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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.util.Log;

/**
 * Some utilities for dealing with raw byte array scan records.
 */
public final class Utils_Reflection extends Utils
{
	private final static Map<Class<?>, Class<?>> BOXED_PRIMITIVES = new HashMap<Class<?>, Class<?>>()
	{
		{
			put(Integer.class, int.class);
			put(Long.class, long.class);
			put(Short.class, short.class);
			put(Byte.class, byte.class);
			put(Float.class, float.class);
			put(Double.class, double.class);
			put(Boolean.class, boolean.class);
			put(Character.class, char.class);
		}
	};

	private Utils_Reflection(){super();}

	private static final String TAG = Utils_Reflection.class.getName();
	
	public static String fieldStringValue(Field field)
	{
		String uuidString = "";

		if (field != null)
		{
			Object uuid = staticFieldValue(field);
			if (uuid instanceof String)
			{
				uuidString = (String) uuid;
			}
			else if (uuid instanceof UUID)
			{
				uuidString = uuid.toString();
			}
			uuidString = uuidString.toLowerCase(Locale.US);
		}
		
		return uuidString;
	}
	
	public static <T extends Object> T staticFieldValue(Field field)
	{
		Object value = null;
		
		try {
			value = field.get(null);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		
		return (T) value;
	}
	
	public static <T> void nullOut(T target, Class<? extends T> type)
	{
		Class<?> clazz = type;
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields)
		{
			if( Modifier.isStatic(field.getModifiers()) )  continue;
			
//			Log.e("", field.getName()+"");
			
			try
			{
				field.set(target, null);
			}
			catch (IllegalAccessException e)
			{
//				e.printStackTrace();
			}
			catch (IllegalArgumentException e)
			{
//				e.printStackTrace();
			}
		}
	}

	public static boolean callBooleanReturnMethod(final Object instance, final String methodName, boolean showError)
	{
		return callBooleanReturnMethod(instance, methodName, null, showError);
	}

	public static boolean callBooleanReturnMethod(final Object instance, final String methodName, final Class[] paramTypes, boolean showError, final Object ... params)
	{
		try
		{
			final Method method = instance.getClass().getMethod(methodName, paramTypes);
			final Boolean result = (Boolean) method.invoke(instance, params);

			if( result == null || !result )
			{
				return false;
			}
		}
		catch (Exception e)
		{
			if (showError)
			{
				Log.e("SweetBlue", "Problem calling method: " + methodName + " - " + e);
			}

			return false;
		}

		return true;
	}



	// Added for SweetDI
	public static <T> T constructInstance(Class<T> implementationClass, FunctionIO<Class<?>, ?> externalConstructorFunction, Object... args)
	{
		int mods = implementationClass.getModifiers();
		boolean isInnerClass = implementationClass.getEnclosingClass() != null;

		T instance = null;
		if (!isInnerClass && (args == null || args.length == 0))
			instance = constructParameterlessInstance(implementationClass);

		if (instance != null) return instance;

		instance = constructWithParameters(implementationClass, new Pointer<>(args), externalConstructorFunction);

		if (Modifier.isAbstract(mods) && instance == null)
			throw new RuntimeException("Cannot construct the abstract class " + implementationClass);

		return instance;
	}


	private static boolean containsTypes(Class<?>[] actualClasses, Class<?>[] classesLookingFor)
	{
		int otherIndex = actualClasses.length - 1;
		for (int i = classesLookingFor.length - 1; i >= 0; i--)
		{
			Class<?> check = classesLookingFor[i];
			Class<?> clazz = actualClasses[otherIndex--];
			if (clazz != check && !clazz.isAssignableFrom(check))
				return false;
		}
		return true;
	}

	private static boolean hydrateArgTypes(Class<?>[] argTypes, Object[] args)
	{
		boolean hasPrimitives = false;
		if (args != null)
		{
			for (int i = 0; i < args.length; i++)
			{
				Object arg = args[i];
				argTypes[i] = arg.getClass();
				if (BOXED_PRIMITIVES.containsKey(argTypes[i])) hasPrimitives = true;
			}
		}
		return hasPrimitives;
	}

	@SuppressWarnings("unchecked")
	private static <T> Constructor<T> getConstructor(Class<T> implementationClass, Pointer<Class<?>[]> argTypes, Pointer<Object[]> parameters, FunctionIO<Class<?>, ?> externalConstructorFunction)
	{
		// First, try to get the constructor from the given argTypes
		Constructor<T> constructor = getConstructor_simple(implementationClass, argTypes.value);

		if (constructor != null)	return constructor;

		// Now we'll get all constructors and look through each of them to see if any match. One of them must have the types
		// in the order they are passed into this function.
		Constructor<T>[] constructors = (Constructor<T>[]) implementationClass.getDeclaredConstructors();
		for (Constructor<T> constructor1 : constructors)
		{
			Class<?>[] paramTypes = constructor1.getParameterTypes();
			if (!containsTypes(paramTypes, argTypes.value))
				continue;


			// The following should always be at least 1, as if the length of the to arrays are the same, it should have
			// already been constructed earlier in this method
			int stopIndex = paramTypes.length - argTypes.value.length;
			boolean canProceed = true;
			boolean isInnerClass = implementationClass.getEnclosingClass() != null;
			List<Object> innerDeps = new ArrayList<>();
			for (int i = 0; i < stopIndex; i++)
			{
				Class<?> paramClass = paramTypes[i];
				if (isInnerClass && i == 0)
				{
					Class<?> enclosingClass = implementationClass.getEnclosingClass();
					if (!paramClass.equals(enclosingClass))
					{
						canProceed = false;
						break;
					}
					// TODO - Error handling...what if this fails?
					Object instance = constructParameterlessInstance(paramClass);
					innerDeps.add(instance);
				}
				else
				{
					// TODO - Error handling if the call returns null
					if (externalConstructorFunction != null)
					{
						Object instance = externalConstructorFunction.call(paramClass);
						innerDeps.add(instance);
					}
				}
			}
			innerDeps.addAll(Arrays.asList(parameters.value));
			parameters.value = innerDeps.toArray();
			if (canProceed)
			{
				constructor = constructor1;
				break;
			}
		}
		return constructor;
	}

	private static <T> Constructor<T> getConstructor_simple(Class<T> implementationClass, Class<?>[] argTypes)
	{
		Constructor<T> constructor = null;
		try
		{
			constructor = implementationClass.getDeclaredConstructor(argTypes);
		}
		catch (Exception e) {}

		return constructor;
	}

	private static <T> T constructWithParameters(Class<T> implementationClass, Pointer<Object[]> parametersPointer, FunctionIO<Class<?>, ?> externalConstructorFunction)
	{
		try
		{
			Class<?>[] argTypes = new Class[parametersPointer.value.length];
			Pointer<Class<?>[]> argTypesPointer = new Pointer<>(argTypes);
			boolean hasPrimitives = hydrateArgTypes(argTypes, parametersPointer.value);
			Constructor<T> constructor = getConstructor(implementationClass, argTypesPointer, parametersPointer, externalConstructorFunction);
			if (constructor == null)
			{
				// Try converting the primitives first
				if (hasPrimitives)
				{
					argTypesPointer.value = convertArgTypesToPrimitives_new(argTypes);
					constructor = getConstructor(implementationClass, argTypesPointer, parametersPointer, externalConstructorFunction);
				}

				if (constructor == null)
					throw new RuntimeException("Unable to find constructor for class " + implementationClass);
			}

			boolean accessible = constructor.isAccessible();
			constructor.setAccessible(true);
			T instance = constructor.newInstance(parametersPointer.value);
			constructor.setAccessible(accessible);
			return instance;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private static <T> T constructParameterlessInstance(Class<T> implementationClass)
	{
		try
		{
			return implementationClass.newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private static Class<?>[] convertArgTypesToPrimitives_new(Class<?>[] argTypes)
	{
		Class<?>[] newArgTypes = new Class<?>[argTypes.length];
		for (int i = 0; i < argTypes.length; i++)
		{
			Class<?> argClass = argTypes[i];
			if (BOXED_PRIMITIVES.containsKey(argClass))
				newArgTypes[i] = BOXED_PRIMITIVES.get(argClass);
			else
				newArgTypes[i] = argClass;
		}
		return newArgTypes;
	}
}
