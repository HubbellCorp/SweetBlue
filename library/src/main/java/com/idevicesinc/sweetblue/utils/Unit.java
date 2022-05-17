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

import com.idevicesinc.sweetblue.annotations.Immutable;

/**
 * Common abstract base class for all units of measurement, providing default convenience methods
 * for things like comparison.
 *
 * @param <T> Used to enforce just-in-case subclass type checking for methods like {@link #lt(Unit)} so you can't compare one subclass with another subclass.
 */
@Immutable
public abstract class Unit<T extends Unit>
{
	/**
	 * "less than" comparison.
	 */
	public boolean lt(final Unit<T> otherUnit)
	{
		return this.getRawValue() < otherUnit.getRawValue();
	}
	
	/**
	 * "less than or equal" comparison.
	 */
	public boolean lte(final Unit<T> otherUnit)
	{
		return this.getRawValue() <= otherUnit.getRawValue();
	}
	
	/**
	 * "greater than" comparison.
	 */
	public boolean gt(final Unit<T> otherUnit)
	{
		return this.getRawValue() > otherUnit.getRawValue();
	}
	
	/**
	 * "greater than or equal" comparison.
	 */
	public boolean gte(final Unit<T> otherUnit)
	{
		return this.getRawValue() >= otherUnit.getRawValue();
	}
	
	/**
	 * "less than" comparison.
	 */
	public boolean lt(final double otherUnit)
	{
		return this.getRawValue() < otherUnit;
	}
	
	/**
	 * "less than or equal" comparison.
	 */
	public boolean lte(final double otherUnit)
	{
		return this.getRawValue() <= otherUnit;
	}
	
	/**
	 * "greater than" comparison.
	 */
	public boolean gt(final double otherUnit)
	{
		return this.getRawValue() > otherUnit;
	}
	
	/**
	 * "greater than or equal" comparison.
	 */
	public boolean gte(final double otherUnit)
	{
		return this.getRawValue() >= otherUnit;
	}
	
	/**
	 * Returns a new instance that is the result of doing <code>this - other</code>.
	 */
	public Unit<T> minus(Unit<T> other)
	{
		if( other == null )  return this;
		
		final double result_raw = this.getRawValue() - other.getRawValue();
		
		return newInstance(result_raw);
	}
	
	/**
	 * Returns a new instance that is the result of doing <code>this + other</code>.
	 */
	public Unit<T> plus(Unit<T> other)
	{
		if( other == null )  return this;
		
		final double result_raw = this.getRawValue() + other.getRawValue();
		
		return newInstance(result_raw);
	}
	
	/**
	 * Returns a new instance that is the result of doing <code>this * other</code>.
	 */
	public Unit<T> times(Unit<T> other)
	{
		if( other == null )  return newInstance(0.0);
		
		final double result_raw = this.getRawValue() * other.getRawValue();
		
		return newInstance(result_raw);
	}
	
	/**
	 * Returns a new instance that is the result of doing <code>this / other</code>.
	 */
	public Unit<T> dividedBy(Unit<T> other)
	{
		if( other == null )  return newInstance(Double.POSITIVE_INFINITY);
		
		final double result_raw = this.getRawValue() / other.getRawValue();
		
		return newInstance(result_raw);
	}
	
	/**
	 * Hashes {@link #getRawValue()} to an <code>int</code> using {@link Double#hashCode()}.
	 */
	@Override public int hashCode()
	{
		return Double.valueOf(getRawValue()).hashCode();
	}
	
	/**
	 * Returns <code>==</code> using {@link #getRawValue()}.
	 */
	@Override public boolean equals(Object object)
	{
		if( object != null && object instanceof Unit )
		{
			return ((Unit)object).getRawValue() == this.getRawValue();
		}
		
		return super.equals(object);
	}
	
	/**
	 * Subclasses must implement this to return their raw <code>double</code> value.
	 */
	protected abstract double getRawValue();
	
	/**
	 * Subclasses must override this so that arithmetic "operators" can create new instances.
	 */
	protected abstract Unit<T> newInstance(final double rawValue);
}
