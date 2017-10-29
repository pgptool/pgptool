package org.pgptool.gui.ui.checkForUpdates;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;

import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.ui.root.RootPm.CheckForUpdatesDialog;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.ValueAdapterPersistentPropertyImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.tools.edt.Edt;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

public class UpdatesPolicy implements ApplicationContextAware {
	public static final String UPDATES_POLICY = "UpdatesPolicy:";
	public static final String PROP_IS_AUTO = UPDATES_POLICY + "IsAuto";
	public static final String PROP_SNOOZED_VERSION = UPDATES_POLICY + "SnoozedVersion";

	@Autowired
	private ConfigPairs configPairs;
	private ApplicationContext applicationContext;
	private CheckForUpdatesPm checkForUpdatesPm;
	private CheckForUpdatesDialog checkForUpdatesDialog;

	private ModelProperty<Boolean> isAutoUpdatesEnabled;
	private ValueAdapter<String> snoozedVersion;

	public void start(CheckForUpdatesDialog checkForUpdatesDialog) {
		this.checkForUpdatesDialog = checkForUpdatesDialog;
		this.snoozedVersion = new ValueAdapterPersistentPropertyImpl<String>(configPairs, PROP_SNOOZED_VERSION, null);

		isAutoUpdatesEnabled = new ModelProperty<>(this,
				new ValueAdapterPersistentPropertyImpl<Boolean>(configPairs, PROP_IS_AUTO, null), PROP_IS_AUTO);
		if (isAutoUpdatesEnabled.getValue() == null) {
			isAutoUpdatesEnabled.setValueByOwner(UiUtils.confirm("prompt.doAutoUpdatesCheck", null, null));
		}

		if (Boolean.FALSE.equals(isAutoUpdatesEnabled.getValue())) {
			return;
		}

		if (Boolean.TRUE.equals(isAutoUpdatesEnabled.getValue())) {
			// NOTE: This will trigger check for updates and since this Pm is singleton when
			// result arrive view will just render already prepared results
			checkForUpdatesPm = applicationContext.getBean(CheckForUpdatesPm.class);
			checkForUpdatesPm.getNewVersion().addPropertyChangeListener(onNewVersionLinkChanged);
			checkForUpdatesPm.init(checkForUpdatesDialog.host, this);
		}
	}

	@SuppressWarnings("serial")
	public final Action actionAutoCheckForUpdates = new LocalizedAction("action.autoCheckUpdates") {
		@Override
		public void actionPerformed(ActionEvent e) {
			isAutoUpdatesEnabled.setValueByOwner(!isAutoUpdatesEnabled.getValue());
		}
	};

	private PropertyChangeListener onNewVersionLinkChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (!StringUtils.hasText("" + evt.getNewValue())) {
				return;
			}
			if (evt.getNewValue().equals(snoozedVersion.getValue())) {
				return;
			}
			checkForUpdatesDialog.actionToOpenWindow.actionPerformed(null);
		}
	};

	public void snoozeVersion(String versionToSnooze) {
		snoozedVersion.setValue(versionToSnooze);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ModelPropertyAccessor<Boolean> getIsAutoUpdatesEnabled() {
		return isAutoUpdatesEnabled.getModelPropertyAccessor();
	}
}
