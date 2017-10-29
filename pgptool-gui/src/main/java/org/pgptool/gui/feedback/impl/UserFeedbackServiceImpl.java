package org.pgptool.gui.feedback.impl;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.feedback.api.UserFeedback;
import org.pgptool.gui.feedback.api.UserFeedbackService;
import org.pgptool.gui.tools.HttpTools;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.gson.Gson;

public class UserFeedbackServiceImpl implements UserFeedbackService {
	private static Logger log = Logger.getLogger(UserFeedbackServiceImpl.class);

	private String endpoint;

	private Gson gson = new Gson();

	@Override
	public boolean isFeedbackApiAvailable() {
		try {
			String json = HttpTools.httpGet(endpoint, null);
			FeedbackApiResponse response = gson.fromJson(json, FeedbackApiResponse.class);
			Preconditions.checkState("available".equals(response.getResult()),
					"Availability result returned unexpected value: " + response.getResult());
			return true;
		} catch (Throwable t) {
			log.warn("Failed to check feedback API availability", t);
			return false;
		}
	}

	@Override
	public void submit(UserFeedback userFeedback) throws FieldValidationException, GenericException {
		try {
			validate(userFeedback);
			String json = HttpTools.httpPost(endpoint, gson.toJson(userFeedback));
			FeedbackApiResponse response = gson.fromJson(json, FeedbackApiResponse.class);
			Preconditions.checkState("ok".equals(response.getResult()),
					"Form submission returned unexpected response: " + response.getResult());
		} catch (Throwable t) {
			Throwables.propagateIfInstanceOf(t, FieldValidationException.class);
			throw new GenericException("exception.failedToSubmitFeedback", t);
		}
	}

	private void validate(UserFeedback userFeedback) throws FieldValidationException {
		ValidationContext ctx = new ValidationContext();
		if (StringUtils.hasText(userFeedback.getEmail())) {
			ctx.validateEmailFormat(userFeedback.getEmail(), UserFeedback.FN_EMAIL);
		}
		ctx.throwIfHasErrors();
	}

	public String getEndpoint() {
		return endpoint;
	}

	@Required
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
}
