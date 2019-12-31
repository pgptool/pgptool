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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.decryptone.DecryptOneView;
import org.pgptool.gui.ui.decryptonedialog.DecryptOneDialogPm.Intent;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordManyKeysView;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordOneKeyView;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPm;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ru.skarpushin.swingpm.base.ViewBase;
import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;

public class DecryptOneDialogView extends DialogViewBaseCustom<DecryptOneDialogPm> implements ApplicationContextAware {
	private DecryptOneView decryptOneView;
	private ViewBase<GetKeyPasswordPm> passwordView;

	private JPanel pnl;
	private Intent currentIntent = null;

	private JButton currentCancelButton;
	private JButton currentOkButton;
	private Component requestFocusFor;

	private ApplicationContext applicationContext;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		currentIntent = null;
		bindingContext.registerOnChangeHandler(pm.getIntent(), intentChanged);
		intentChanged.handlePropertyChanged(this, null, null, pm.getIntent().getValue());
	}

	@Override
	public void unrender() {
		if (passwordView != null) {
			passwordView.unrender();
			passwordView = null;
		}
		if (decryptOneView != null) {
			decryptOneView.unrender();
			decryptOneView = null;
		}
		super.unrender();
	}

	private TypedPropertyChangeListener<Intent> intentChanged = new TypedPropertyChangeListener<DecryptOneDialogPm.Intent>() {
		@Override
		public void handlePropertyChanged(Object source, String propertyName, Intent oldValue, Intent newValue) {
			if (currentIntent == newValue) {
				return;
			}
			currentIntent = newValue;
			if (newValue == Intent.Decrypt) {
				if (passwordView != null) {
					passwordView.unrender();
					passwordView = null;
				}
				DecryptOneView subView = getDecryptOneView();
				subView.renderTo(pnl, BorderLayout.CENTER);
				currentCancelButton = subView.btnCancel;
				currentOkButton = subView.btnPerformOperation;
				requestFocusFor = currentOkButton;
			} else if (newValue == Intent.PasswordRequest) {
				if (decryptOneView != null) {
					decryptOneView.unrender();
					decryptOneView = null;
				}
				getPasswordView().renderTo(pnl, BorderLayout.CENTER);
				if (getPasswordView() instanceof GetKeyPasswordOneKeyView) {
					GetKeyPasswordOneKeyView subView = (GetKeyPasswordOneKeyView) getPasswordView();
					currentCancelButton = subView.btnCancel;
					currentOkButton = subView.btnPerformOperation;
					requestFocusFor = subView.edPassword;
				} else if (getPasswordView() instanceof GetKeyPasswordManyKeysView) {
					GetKeyPasswordManyKeysView subView = (GetKeyPasswordManyKeysView) getPasswordView();
					currentCancelButton = subView.btnCancel;
					currentOkButton = subView.btnPerformOperation;
					requestFocusFor = subView.edPassword;
				}
			} else {
				throw new RuntimeException("Intent not supported " + newValue);
			}

			// IMPORTANT: We have to do it on another event cycle so that window
			// will be sized properly
			SwingUtilities.invokeLater(() -> {
				if (dialog != null) {
					dialog.pack();
					if (currentOkButton != null) {
						dialog.getRootPane().setDefaultButton(currentOkButton);
					}
					if (requestFocusFor != null) {
						requestFocusFor.requestFocusInWindow();
					}
					// UiUtils.centerWindow(dialog); // THIS WILL SCREW UP
					// WINDOW SIZE. WHY????!!?!?!?!?!
				}
			});
		}
	};

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.MODELESS);
		ret.setLayout(new BorderLayout());
		ret.setResizable(false);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("action.decrypt"));
		ret.add(pnl, BorderLayout.CENTER);
		ret.pack();
		UiUtils.centerWindow(ret, owner);
		return ret;
	}

	@Override
	protected void handleDialogShown() {
		super.handleDialogShown();
		if (requestFocusFor != null) {
			requestFocusFor.requestFocusInWindow();
		}
	}

	@Override
	protected JPanel getRootPanel() {
		return pnl;
	}

	@Override
	protected void dispatchWindowCloseEvent(ActionEvent originAction) {
		if (currentCancelButton != null) {
			currentCancelButton.getAction().actionPerformed(originAction);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public DecryptOneView getDecryptOneView() {
		if (decryptOneView == null) {
			decryptOneView = applicationContext.getBean(DecryptOneView.class);
			decryptOneView.setPm(pm.getDecryptOnePm());
		}
		return decryptOneView;
	}

	public ViewBase<GetKeyPasswordPm> getPasswordView() {
		if (passwordView == null) {
			if (pm.getGetKeyPasswordPm().getMatchedKeys() != null
					&& pm.getGetKeyPasswordPm().getMatchedKeys().size() == 1) {
				passwordView = applicationContext.getBean(GetKeyPasswordOneKeyView.class);
			} else {
				passwordView = applicationContext.getBean(GetKeyPasswordManyKeysView.class);
			}
			passwordView.setPm(pm.getGetKeyPasswordPm());
		}
		return passwordView;
	}

}
