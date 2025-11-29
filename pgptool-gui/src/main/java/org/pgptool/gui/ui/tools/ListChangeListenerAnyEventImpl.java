/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package org.pgptool.gui.ui.tools;

import ru.skarpushin.swingpm.collections.ListExEventListener;

/**
 * Simple listener that is used when all we care about was listed changed and we on't need to know
 * specifics
 *
 * @author Sergey Karpushin
 * @param <T>
 */
public abstract class ListChangeListenerAnyEventImpl<T> implements ListExEventListener<T> {
  public abstract void onListChanged();

  @Override
  public void onItemAdded(T item, int atIndex) {
    onListChanged();
  }

  @Override
  public void onItemChanged(T item, int atIndex) {
    onListChanged();
  }

  @Override
  public void onItemRemoved(T item, int wasAtIndex) {
    onListChanged();
  }

  @Override
  public void onAllItemsRemoved(int sizeWas) {
    onListChanged();
  }
}
