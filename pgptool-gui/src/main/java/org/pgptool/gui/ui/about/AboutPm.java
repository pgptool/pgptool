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
package org.pgptool.gui.ui.about;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.Action;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

public class AboutPm extends PresentationModelBase implements InitializingBean {
	private static Logger log = Logger.getLogger(AboutPm.class);

	private ModelProperty<String> version;
	private ModelProperty<String> linkToSite;

	@Value("${net.ts.baseUrl}")
	private String urlToSite;

	private AboutHost host;

	@Override
	public void afterPropertiesSet() throws Exception {
		version = new ModelProperty<String>(this, new ValueAdapterReadonlyImpl<String>(retVersion()), "version");
		linkToSite = new ModelProperty<String>(this, new ValueAdapterReadonlyImpl<String>(urlToSite), "linkToSite");
	}

	private static String retVersion() {
		try {
			String ret = AboutPm.class.getPackage().getImplementationVersion();
			Preconditions.checkState(StringUtils.hasText(ret),
					"ImplementationVersion cannot be resolved. Perhaps we're in the DEV mode");
			return ret;
		} catch (Throwable t) {
			log.warn("Failed to resolve current application version", t);
			return "unresolved";
		}
	}

	public void init(AboutHost host) {
		this.host = host;
	}

	@SuppressWarnings("serial")
	protected final Action actionClose = new LocalizedAction("action.close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleClose();
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionOpenSite = new LocalizedAction("term.linkToSite") {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Desktop.getDesktop().browse(new URI(urlToSite));
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("exception.unexpected", t);
			}
		}
	};

	public ModelPropertyAccessor<String> getVersion() {
		return version.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getLinkToSite() {
		return linkToSite.getModelPropertyAccessor();
	}

}
