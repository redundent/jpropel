// /////////////////////////////////////////////////////////
// This file is part of Propel.
//
// Propel is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Propel is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with Propel. If not, see <http://www.gnu.org/licenses/>.
// /////////////////////////////////////////////////////////
// Authored by: Nikolaos Tountas -> salam.kaser-at-gmail.com
// /////////////////////////////////////////////////////////
package propel.core.initialisation;

import propel.core.threading.primitives.SharedFlag;
import lombok.Validate;
import lombok.Validate.NotNull;

/**
 * Thread-safe lock-free guard, allows components to check whether they have initialised prior to being used. Also prevents multiple
 * initialisations.
 */
public final class SingleInitGuard
    implements InitGuard
{
  // used for maintaining the state
  private final SharedFlag flag;
  // used to identify the owner of this guard
  private final String className;

  /**
   * Constructor
   * 
   * @throws NullPointerException An argument is null
   */
  @Validate
  public SingleInitGuard(@NotNull final Class<?> owner)
  {
    this.className = owner.getSimpleName();
    this.flag = new SharedFlag();
  }

  /**
   * Call to initialise, can be called once
   */
  public void initialise()
  {
    if (!flag.set())
      throw new IllegalStateException("This method is to be called exactly once per program execution");
  }

  /**
   * {@inheritDoc}
   */
  public void assertInitialised()
  {
    if (flag.isNotSet())
      throw new IllegalStateException("Cannot proceed with program execution without previously initialising the class " + className);
  }

  /**
   * {@inheritDoc}
   */
  public void uninitialise()
  {
    flag.unSet();
  }

}