package org.pgptool.gui.feedback.api;

import org.pgptool.gui.app.GenericException;
import org.summerb.approaches.validation.FieldValidationException;

public interface UserFeedbackService {
	boolean isFeedbackApiAvailable();

	void submit(UserFeedback userFeedback) throws FieldValidationException, GenericException;
}
