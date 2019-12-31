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
package org.pgptool.gui.ui.decryptonedialog;

import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.Action;

import org.pgptool.gui.app.Message;
import org.pgptool.gui.ui.decryptone.DecryptOneHost;
import org.pgptool.gui.ui.decryptone.DecryptOnePm;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordHost;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPm;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPmInitResult;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.pgptool.gui.ui.getkeypassworddialog.GetKeyPasswordDialogPm.GetKeyPasswordPo;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ru.skarpushin.swingpm.EXPORT.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

/**
 * This component is a container that will change it's appearance on the fly
 * between {@link GetKeyPasswordPm} and {@link DecryptOnePm} to provide more
 * streamlined UX
 */
public class DecryptOneDialogPm extends PresentationModelBase<DecryptOneDialogHost, String>
		implements ApplicationContextAware {
	public static enum Intent {
		Decrypt, PasswordRequest;
	};

	@Autowired
	private DecryptOnePm decryptOnePm;
	private GetKeyPasswordPm getKeyPasswordPm;

	private ModelProperty<Intent> intent;

	private KeyAndPasswordCallback keyAndPasswordCallback;
	private PasswordDeterminedForKey passwordDeterminedForKey;

	@Override
	public boolean init(ActionEvent originAction, DecryptOneDialogHost host, String optionalSource) {
		super.init(originAction, host, optionalSource);
		intent = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(Intent.Decrypt), "intent");
		return decryptOnePm.init(originAction, decryptOneHost, optionalSource);
	}

	private DecryptOneHost decryptOneHost = new DecryptOneHost() {
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
				KeyAndPasswordCallback keyAndPasswordCallback) {
			DecryptOneDialogPm.this.keyAndPasswordCallback = keyAndPasswordCallback;

			getKeyPasswordPm = null; // force this PM to re-init
			// NOTE: Instead of propagating actual event we construct surrogate one because
			// this request came from child component
			ActionEvent surrogateEvent = UiUtils.actionEvent(findRegisteredWindowIfAny(), "askUserForKeyAndPassword");
			GetKeyPasswordPmInitResult initResult = getGetKeyPasswordPm().initEx(surrogateEvent, getPasswordHost,
					new GetKeyPasswordPo(sourceFileRecipientsKeysIds, purpose, keyAndPasswordCallback));
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
		@Override
		public void onPasswordDeterminedForKey(PasswordDeterminedForKey result) {
			passwordDeterminedForKey = (PasswordDeterminedForKey) result;
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
