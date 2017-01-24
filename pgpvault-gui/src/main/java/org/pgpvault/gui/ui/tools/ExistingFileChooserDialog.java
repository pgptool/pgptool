package org.pgpvault.gui.ui.tools;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.configpairs.api.ConfigPairs;
import org.pgpvault.gui.tools.PathUtils;
import org.springframework.util.StringUtils;

public class ExistingFileChooserDialog {
	private static Logger log = Logger.getLogger(ExistingFileChooserDialog.class);

	private Component optionalParent;
	private ConfigPairs configPairs;
	private String configPairNameToRemember;

	public ExistingFileChooserDialog(Component optionalParent, ConfigPairs configPairs,
			String configPairNameToRemember) {
		this.optionalParent = optionalParent;
		this.configPairs = configPairs;
		this.configPairNameToRemember = configPairNameToRemember;
	}

	public String askUserForFile() {
		JFileChooser ofd = buildSourceFileChooserDialog();

		int result = ofd.showOpenDialog(optionalParent);
		if (result != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File retFile = ofd.getSelectedFile();
		if (retFile == null) {
			return null;
		}

		String ret = retFile.getAbsolutePath();
		configPairs.put(configPairNameToRemember, PathUtils.extractBasePath(ret));
		return ret;
	}

	private JFileChooser buildSourceFileChooserDialog() {
		JFileChooser ofd = new JFileChooser();
		ofd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		ofd.setAcceptAllFileFilterUsed(true);
		ofd.setMultiSelectionEnabled(false);
		ofd.setDialogTitle(Messages.get("action.chooseExistingFile"));
		ofd.setApproveButtonText(Messages.get("action.choose"));
		suggestInitialDirectoryForSource(ofd);
		doFileChooserPostConstruct(ofd);
		return ofd;
	}

	protected void doFileChooserPostConstruct(JFileChooser ofd) {
		// NOTE: It's meant to be overridden. Subclass can augment chooser
		// if needed
	}

	protected void suggestInitialDirectoryForSource(JFileChooser ofd) {
		try {
			String lastChosenDestination = configPairs.find(configPairNameToRemember, null);
			String pathname = StringUtils.hasText(lastChosenDestination) ? lastChosenDestination
					: SystemUtils.getUserHome().getAbsolutePath();
			ofd.setCurrentDirectory(new File(pathname));
		} catch (Throwable t) {
			log.warn("Failed to set suggested location by key " + configPairNameToRemember, t);
		}
	}
}
