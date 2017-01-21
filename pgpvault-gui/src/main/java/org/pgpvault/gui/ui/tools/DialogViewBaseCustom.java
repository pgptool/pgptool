package org.pgpvault.gui.ui.tools;

import java.awt.Image;
import java.util.List;

import ru.skarpushin.swingpm.base.DialogViewBase;
import ru.skarpushin.swingpm.base.PresentationModel;

public abstract class DialogViewBaseCustom<TPM extends PresentationModel> extends DialogViewBase<TPM> {
	@Override
	protected List<Image> getWindowIcon() {
		return WindowIcon.getWindowIcon();
	}
}
