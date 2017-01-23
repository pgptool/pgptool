package org.pgpvault.gui.ui.keyslist;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.Action;

import org.pgpvault.gui.encryption.api.KeyRingService;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;
import org.pgpvault.gui.ui.tools.UiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.table.ModelTableProperty;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class KeysListPm extends PresentationModelBase {
	@Autowired
	private EventBus eventBus;
	@Autowired
	@Resource(name = "keyRingService")
	private KeyRingService<KeyData> keyRingService;
	private KeysListHost host;

	private ModelTableProperty<Key<KeyData>> tableModelProp;
	private ModelProperty<Boolean> hasData;

	public void init(KeysListHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		List<Key<KeyData>> initialKeys = keyRingService.readKeys();
		tableModelProp = new ModelTableProperty<>(this, initialKeys, "keys", new KeysTableModel());
		hasData = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(!initialKeys.isEmpty()), "hasData");
		tableModelProp.getModelPropertyAccessor().addPropertyChangeListener(onSelectionChangedHandler);
		onSelectionChangedHandler.propertyChange(null);

		eventBus.register(this);
	}

	private PropertyChangeListener onSelectionChangedHandler = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			actionDeleteKey.setEnabled(tableModelProp.hasValue());
		}
	};

	@Subscribe
	public void onRowChangedEvent(EntityChangedEvent<?> event) {
		if (!event.isTypeOf(Key.class)) {
			return;
		}

		List<Key<KeyData>> newKeysList = keyRingService.readKeys();
		tableModelProp.getList().clear();
		tableModelProp.getList().addAll(newKeysList);
		hasData.setValueByOwner(!newKeysList.isEmpty());

		// NOTE: Selection is not nicely maintained. Each update will clear the
		// current selection if any
	}

	@Override
	public void detach() {
		super.detach();
		eventBus.unregister(this);
	}

	@SuppressWarnings("serial")
	private Action actionClose = new LocalizedAction("action.close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleClose();
		}
	};

	@SuppressWarnings("serial")
	private Action actionActivate = new LocalizedAction("action.activate") {
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Impl
		}
	};

	@SuppressWarnings("serial")
	private Action actionDeleteKey = new LocalizedAction("action.deleteKey") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!tableModelProp.hasValue()) {
				// silently exit -- not going to complain
				return;
			}

			Key<KeyData> key = tableModelProp.getValue();
			if (!UiUtils.confirm("phrase.areYouSureToDeleteKey", new Object[] { key.getKeyInfo().getUser() },
					findRegisteredWindowIfAny())) {
				return;
			}

			keyRingService.removeKey(key);
		}
	};

	private Action[] contextMenuActions = new Action[] { actionDeleteKey };

	protected Action getActionClose() {
		return actionClose;
	}

	protected Action getActionImport() {
		return host.getActionImportKey();
	}

	public Action getActionForRowDoubleClick() {
		return actionActivate;
	}

	public ModelTablePropertyAccessor<Key<KeyData>> getTableModelProp() {
		return tableModelProp.getModelTablePropertyAccessor();
	}

	public ModelPropertyAccessor<Key<KeyData>> getSelectedRow() {
		return tableModelProp.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getHasData() {
		return hasData.getModelPropertyAccessor();
	}

	public Action getActionDelete() {
		return actionDeleteKey;
	}

	public Action[] getContextMenuActions() {
		return contextMenuActions;
	}
}
