package org.pgptool.gui.ui.tools;

import org.pgptool.gui.bkgoperation.Progress;
import org.pgptool.gui.bkgoperation.ProgressHandler;

import ru.skarpushin.swingpm.modelprops.ModelProperty;

public class ProgressHandlerPmMixinImpl implements ProgressHandler {
	private ModelProperty<Boolean> isProgressVisible;
	private ModelProperty<Integer> progressValue;
	private ModelProperty<String> progressNote;

	public ProgressHandlerPmMixinImpl(ModelProperty<Boolean> isProgressVisible, ModelProperty<Integer> progressValue,
			ModelProperty<String> progressNote) {
		super();
		this.isProgressVisible = isProgressVisible;
		this.progressValue = progressValue;
		this.progressNote = progressNote;
	}

	@Override
	public void onProgressUpdated(Progress progress) {
		if (Thread.interrupted()) {
			progress.requestCancelation();
		}

		if (!isProgressVisible.getValue()) {
			progressNote.setValueByOwner("");
			progressValue.setValueByOwner(0);
			isProgressVisible.setValueByOwner(true);
		} else {
			int percentage = progress.getPercentage() == null ? 0 : progress.getPercentage();
			progressValue.setValueByOwner(percentage);
			progressNote.setValueByOwner("" + percentage + "%");
			// progressNote.setValueByOwner(
			// "" + percentage + "% " + Messages.text(progress.getStepCode(),
			// progress.getStepArgs()));
		}

		if (progress.isCompleted()) {
			isProgressVisible.setValueByOwner(false);
		}
	}
}