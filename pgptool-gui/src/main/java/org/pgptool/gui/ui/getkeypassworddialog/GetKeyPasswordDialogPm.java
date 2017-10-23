package org.pgptool.gui.ui.getkeypassworddialog;

import java.util.Set;

import org.pgptool.gui.app.Message;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.ui.decryptonedialog.KeyAndPasswordCallback;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordHost;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPm;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPmInitResult;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ru.skarpushin.swingpm.base.PresentationModelBase;

/**
 * This component is a container that will change it's appearance on the fly
 * between {@link GetKeyPasswordPm} and {@link GetKeyPasswordPm} to provide more
 * streamlined UX
 */
public class GetKeyPasswordDialogPm extends PresentationModelBase implements ApplicationContextAware {
	@Autowired
	private GetKeyPasswordPm getKeyPasswordPm;

	private GetKeyPasswordDialogHost host;
	@SuppressWarnings("rawtypes")
	private KeyAndPasswordCallback keyAndPasswordCallback;

	public boolean init(GetKeyPasswordDialogHost host, Set<String> keysIds, Message purpose,
			KeyAndPasswordCallback<?> keyAndPasswordCallback) {
		this.host = host;
		this.keyAndPasswordCallback = keyAndPasswordCallback;
		
		GetKeyPasswordPmInitResult result = getKeyPasswordPm.init(getPasswordHost, keysIds, purpose);
		if (result == GetKeyPasswordPmInitResult.NoMatchingKeys) {
			keyAndPasswordCallback.onKeyPasswordResult(null);
			return false;
		}

		if (result == GetKeyPasswordPmInitResult.CachedPasswordFound) {
			// also not showing UI. Callback was already called
			return false;
		}

		return true;
	}

	private GetKeyPasswordHost getPasswordHost = new GetKeyPasswordHost() {
		@SuppressWarnings("unchecked")
		@Override
		public <T extends KeyData> void onPasswordDeterminedForKey(PasswordDeterminedForKey<T> result) {
			host.handleClose();
			keyAndPasswordCallback.onKeyPasswordResult(result);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onCancel() {
			host.handleClose();
			keyAndPasswordCallback.onKeyPasswordResult(null);
		}
	};

	private ApplicationContext applicationContext;

	public GetKeyPasswordPm getGetKeyPasswordPm() {
		if (getKeyPasswordPm == null) {
			getKeyPasswordPm = applicationContext.getBean(GetKeyPasswordPm.class);
		}
		return getKeyPasswordPm;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
