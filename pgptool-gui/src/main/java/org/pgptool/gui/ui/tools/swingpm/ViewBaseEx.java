package org.pgptool.gui.ui.tools.swingpm;

import org.springframework.beans.factory.InitializingBean;

import ru.skarpushin.swingpm.base.PresentationModel;
import ru.skarpushin.swingpm.base.ViewBase;

public abstract class ViewBaseEx<TPM extends PresentationModel> extends ViewBase<TPM> implements InitializingBean {

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

}
