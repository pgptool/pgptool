package org.pgpvault.gui.ui.tools;

import ru.skarpushin.swingpm.collections.ListExEventListener;

/**
 * Simple listener that is used when all we care about was listed changed and we
 * on't need to know specifics
 * 
 * @author sergeyk
 *
 * @param <T>
 */
public abstract class ListChangeListenerAnyEventImpl<T> implements ListExEventListener<T> {
	abstract public void onListChanged();

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
