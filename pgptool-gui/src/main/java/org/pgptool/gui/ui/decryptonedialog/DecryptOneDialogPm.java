package org.pgptool.gui.ui.decryptonedialog;

import java.util.Set;

import javax.swing.Action;

import org.pgptool.gui.app.Message;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.ui.decryptone.DecryptOneHost;
import org.pgptool.gui.ui.decryptone.DecryptOnePm;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordHost;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPm;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPmInitResult;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class DecryptOneDialogPm extends PresentationModelBase implements ApplicationContextAware {
	public static enum Intent {
		Decrypt, PasswordRequest;
	};

	@Autowired
	private DecryptOnePm decryptOnePm;
	private GetKeyPasswordPm getKeyPasswordPm;

	private DecryptOneDialogHost host;
	private ModelProperty<Intent> intent;

	private KeyAndPasswordCallback<KeyData> keyAndPasswordCallback;
	private PasswordDeterminedForKey<KeyData> passwordDeterminedForKey;

	public boolean init(DecryptOneDialogHost host, String optionalSource) {
		this.host = host;

		intent = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(Intent.Decrypt), "intent");

		return decryptOnePm.init(decryptOneHost, optionalSource);
	}

	private DecryptOneHost<KeyData> decryptOneHost = new DecryptOneHost<KeyData>() {
		@Override
		public void handleClose() {
			host.handleClose();
		}

		@Override
		public Action getActionToOpenCertificatesList() {
			return host.getActionToOpenCertificatesList();
		}

		@Override
		public void askUserForKeyAndPassword(Set<String> sourceFileRecipientsKeysIds, Message purpose,
				KeyAndPasswordCallback<KeyData> keyAndPasswordCallback) {
			DecryptOneDialogPm.this.keyAndPasswordCallback = keyAndPasswordCallback;

			getKeyPasswordPm = null; // force this PM to re-init
			GetKeyPasswordPmInitResult initResult = getGetKeyPasswordPm().init(getPasswordHost,
					sourceFileRecipientsKeysIds, purpose);
			if (initResult == GetKeyPasswordPmInitResult.CachedPasswordFound) {
				// NOTE: It means getPasswordHost.onPasswordDeterminedForKey was
				// already called somewhere deep in a stack-trace
			} else if (initResult == GetKeyPasswordPmInitResult.NoMatchingKeys) {
				keyAndPasswordCallback.onKeyPasswordResult(null);
				// NOTE: Presuming current intent is Decrypt, no need to change
				// it
			} else {
				intent.setValueByOwner(Intent.PasswordRequest);
			}
		}
	};

	private GetKeyPasswordHost getPasswordHost = new GetKeyPasswordHost() {
		@SuppressWarnings("unchecked")
		@Override
		public <T extends KeyData> void onPasswordDeterminedForKey(PasswordDeterminedForKey<T> result) {
			passwordDeterminedForKey = (PasswordDeterminedForKey<KeyData>) result;
			keyAndPasswordCallback.onKeyPasswordResult(passwordDeterminedForKey);
			intent.setValueByOwner(Intent.Decrypt);
		}

		@Override
		public void onCancel() {
			keyAndPasswordCallback.onKeyPasswordResult(null);
			intent.setValueByOwner(Intent.Decrypt);
		}
	};

	private ApplicationContext applicationContext;

	public ModelPropertyAccessor<Intent> getIntent() {
		return intent.getModelPropertyAccessor();
	}

	public DecryptOnePm getDecryptOnePm() {
		return decryptOnePm;
	}

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
