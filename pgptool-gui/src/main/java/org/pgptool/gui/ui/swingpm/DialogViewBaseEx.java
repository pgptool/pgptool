package org.pgptool.gui.ui.swingpm;

import org.springframework.beans.factory.InitializingBean;

import ru.skarpushin.swingpm.EXPORT.base.DialogViewBase;
import ru.skarpushin.swingpm.base.PresentationModel;

public abstract class DialogViewBaseEx<TPM extends PresentationModel> extends DialogViewBase<TPM>
		implements InitializingBean {

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

}
