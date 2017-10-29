/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
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
 *******************************************************************************/
package org.pgptool.gui.ui.feedbackForm;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.feedback.api.UserFeedback;
import org.pgptool.gui.feedback.api.UserFeedbackService;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.ValueAdapterPersistentPropertyImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationError;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.slider.ModelSliderProperty;
import ru.skarpushin.swingpm.modelprops.slider.ModelSliderPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class FeedbackFormPm extends PresentationModelBase implements InitializingBean {
	@Autowired
	private ConfigPairs configPairs;
	@Autowired
	private UserFeedbackService userFeedbackService;
	private String formUrlForBrowser;

	private FeedbackFormHost host;

	private ModelProperty<Boolean> showConnectingToServerStatus;
	private ModelProperty<Boolean> showFeedbackFrom;
	private ModelProperty<Boolean> feedbackFormDisabled;

	private ModelSliderProperty rating;
	private ModelProperty<String> feedback;
	private ModelProperty<String> email;

	private ListEx<ValidationError> vee = new ListExImpl<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		email = new ModelProperty<String>(this,
				new ValueAdapterPersistentPropertyImpl<String>(configPairs, "email", ""), "email", vee);
		feedback = new ModelProperty<String>(this, new ValueAdapterHolderImpl<String>(""), "feedback", vee);
		rating = new ModelSliderProperty(this, new ValueAdapterHolderImpl<Integer>(7), 1, 10, "rating", vee);

		showConnectingToServerStatus = new ModelProperty<Boolean>(this, new ValueAdapterHolderImpl<Boolean>(true),
				"showTestingConnection");
		showFeedbackFrom = new ModelProperty<Boolean>(this, new ValueAdapterHolderImpl<Boolean>(false),
				"showFeedbackFrom");
		feedbackFormDisabled = new ModelProperty<Boolean>(this, new ValueAdapterHolderImpl<Boolean>(false),
				"feedbackFormDisabled");
	}

	public void init(FeedbackFormHost host) {
		this.host = host;

		actionSubmit.setEnabled(false);
		new Thread(testConnection, "TestConnectionForFeedback").start();
	}

	private Runnable testConnection = new Runnable() {
		@Override
		public void run() {
			boolean available = userFeedbackService.isFeedbackApiAvailable();
			if (available) {
				showFeedbackFrom.setValueByOwner(true);
				actionSubmit.setEnabled(true);
				showConnectingToServerStatus.setValueByOwner(false);
				return;
			}

			redirectUserToBrowser();
			host.handleClose();
		}
	};
	
	@SuppressWarnings("serial")
	protected final Action actionSubmit = new LocalizedAction("action.submit") {
		@Override
		public void actionPerformed(ActionEvent e) {
			feedbackFormDisabled.setValueByOwner(true);
			actionSubmit.setEnabled(false);
			new Thread(submitWorker, "Submit feedback").start();
		}
	};

	protected Runnable submitWorker = new Runnable() {
		@Override
		public void run() {
			try {
				UserFeedback p = new UserFeedback();
				p.setRating(rating.getValue());
				p.setEmail(email.getValue());
				p.setFeedback(feedback.getValue());

				// TODO: Say "we'are uploading"

				userFeedbackService.submit(p);

				// TODO: Show form back

				actionSubmit.setEnabled(true);
				feedbackFormDisabled.setValueByOwner(false);
				vee.clear();

				UiUtils.messageBox(findRegisteredWindowIfAny(), text("term.thankyou"), text("term.confirmation"),
						JOptionPane.INFORMATION_MESSAGE);
				host.handleClose();
			} catch (FieldValidationException fve) {
				actionSubmit.setEnabled(true);
				feedbackFormDisabled.setValueByOwner(false);
				vee.clear();
				vee.addAll(fve.getErrors());
			} catch (Throwable t) {
				UiUtils.reportExceptionToUser("error.failedToSubmitFeedback", t);
				redirectUserToBrowser();
			}
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionCancel = new LocalizedAction("action.cancel") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleClose();
		}
	};

	private void redirectUserToBrowser() {
		try {
			Desktop.getDesktop().browse(new URI(formUrlForBrowser));
		} catch (Throwable t) {
			EntryPoint.reportExceptionToUser("exception.unexpected", t);
		}
	}

	public String getFormUrlForBrowser() {
		return formUrlForBrowser;
	}

	@Required
	public void setFormUrlForBrowser(String formUrlForBrowser) {
		this.formUrlForBrowser = formUrlForBrowser;
	}

	public ModelPropertyAccessor<Boolean> getShowConnectingToServerStatus() {
		return showConnectingToServerStatus.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getShowFeedbackFrom() {
		return showFeedbackFrom.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getFeedbackFormDisabled() {
		return feedbackFormDisabled.getModelPropertyAccessor();
	}

	public ModelSliderPropertyAccessor getRating() {
		return rating.getModelSliderPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getFeedback() {
		return feedback.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getEmail() {
		return email.getModelPropertyAccessor();
	}
}
