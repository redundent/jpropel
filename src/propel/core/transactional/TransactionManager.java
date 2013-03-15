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
package propel.core.transactional;

import static propel.core.functional.projections.MiscProjections.empty;

import java.util.ArrayList;
import java.util.List;

import lombok.Actions.Action0;
import propel.core.common.CONSTANT;
import propel.core.utils.ExceptionUtils;

/**
 * Allows for actions to be executed in a transactional manner, i.e. guarantees that all actions run to completion or rolls back committed
 * actions if an exception is thrown.
 */
public class TransactionManager
    implements ITransactionManager
{
  // the empty action, used when no action/rollback are specified
  private static final Action0 emptyAction = empty();

  /**
   * A list of actions, executed in a way that guarantees complete success or complete failure.
   */
  private final List<Action0> actions;
  /**
   * If an action fails, the corresponding rollback actions are executed
   */
  private final List<Action0> rollbackActions;
  /**
   * The index of the action that is executing. This is useful for determining the index of any actions that fail. This can be then used to
   * call the right rollback action.
   */
  private int executionIndex;

  /**
   * Default constructor
   */
  public TransactionManager()
  {
    actions = new ArrayList<Action0>(16);
    rollbackActions = new ArrayList<Action0>(16);
  }

  /**
   * {@inheritDoc}
   */
  public void clear()
  {
    actions.clear();
    rollbackActions.clear();
    executionIndex = 0;
  }

  /**
   * {@inheritDoc}
   */
  public void add(Action0 action)
  {
    add(action, null);
  }

  /**
   * {@inheritDoc}
   */
  public void add(Action0 action, Action0 rollbackAction)
  {
    if (action == null)
      action = emptyAction;
    if (rollbackAction == null)
      rollbackAction = emptyAction;

    actions.add(action);
    rollbackActions.add(rollbackAction);
  }

  /**
   * {@inheritDoc}
   */
  public void commit()
  {
    // execute all (from start)
    for (executionIndex = 0; executionIndex < actions.size(); executionIndex++)
      actions.get(executionIndex).apply();
  }

  /**
   * {@inheritDoc}
   */
  public void rollback()
  {
    // execute in LIFO style
    for (executionIndex--; executionIndex >= 0; executionIndex--)
      rollbackActions.get(executionIndex).apply();
  }

  /**
   * {@inheritDoc}
   */
  public void commitWithRollback()
      throws Throwable
  {
    try
    {
      commit();
    }
    catch(Throwable e)
    {
      try
      {
        rollback();
      }
      catch(Throwable e2)
      {
        // LLTODO: PropelLog.error("Rollback failed to complete: " + e2);
        throw new Throwable("Rollback failed to complete: " + ExceptionUtils.getMessages(e2, CONSTANT.ENVIRONMENT_NEWLINE)
            + CONSTANT.ENVIRONMENT_NEWLINE + "Original exception: ", e);
      }
      throw e;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void skipCommitStep()
  {
    if (executionIndex < actions.size())
      executionIndex++;
    else
      throw new IllegalStateException("There is no other commit step to skip.");
  }

  /**
   * {@inheritDoc}
   */
  public void skipRollbackStep()
  {
    if (executionIndex > 0)
      executionIndex--;
    else
      throw new IllegalStateException("There is no other rollback step to skip.");
  }

  /**
   * {@inheritDoc}
   */
  public void resumeCommit()
  {
    if (executionIndex < 0 || executionIndex >= rollbackActions.size())
      throw new IndexOutOfBoundsException("The execution index is out of range: " + executionIndex);

    // execute from ExecutionIndex
    for (; executionIndex < actions.size(); executionIndex++)
      actions.get(executionIndex).apply();
  }

  /**
   * {@inheritDoc}
   */
  public void resumeRollback()
  {
    if (executionIndex < 0 || executionIndex >= rollbackActions.size())
      throw new IndexOutOfBoundsException("The execution index is out of range: " + executionIndex);

    // execute in LIFO style (from ExecutionIndex)
    for (; executionIndex >= 0; executionIndex--)
      rollbackActions.get(executionIndex).apply();
  }

  /**
   * A list of actions, executed in a way that guarantees complete success or complete failure.
   */
  public List<Action0> getActions()
  {
    return actions;
  }

  /**
   * If an action fails, the corresponding rollback actions are executed
   */
  public List<Action0> getRollbackActions()
  {
    return rollbackActions;
  }

  /**
   * The index of the action that is executing. This is useful for determining the index of any actions that fail. This can be then used to
   * call the right rollback action.
   */
  public int getExecutionIndex()
  {
    return executionIndex;
  }
}
