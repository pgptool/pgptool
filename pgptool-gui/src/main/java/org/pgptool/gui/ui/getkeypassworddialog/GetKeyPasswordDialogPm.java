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
package org.pgptool.gui.ui.getkeypassworddialog;

import java.util.Set;

import org.pgptool.gui.app.Message;
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
	private KeyAndPasswordCallback keyAndPasswordCallback;

	public boolean init(GetKeyPasswordDialogHost host, Set<String> keysIds, Message purpose,
			KeyAndPasswordCallback keyAndPasswordCallback) {
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
	
	@Override
	public void detach() {
		super.detach();
		getKeyPasswordPm.detach();
	}

	private GetKeyPasswordHost getPasswordHost = new GetKeyPasswordHost() {
		@Override
		public void onPasswordDeterminedForKey(PasswordDeterminedForKey result) {
			host.handleClose();
			keyAndPasswordCallback.onKeyPasswordResult(result);
		}

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
