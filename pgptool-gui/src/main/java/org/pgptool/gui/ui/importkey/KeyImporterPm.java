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
package org.pgptool.gui.ui.importkey;

import static org.pgptool.gui.app.Messages.text;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.ui.keyslist.ComparatorKeyByNameImpl;
import org.pgptool.gui.ui.keyslist.KeysTableModel;
import org.pgptool.gui.ui.keyslist.KeysTablePm;
import org.pgptool.gui.ui.tools.MultipleFilesChooserDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.base.View;
import ru.skarpushin.swingpm.modelprops.table.ModelTableProperty;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;

public class KeyImporterPm extends PresentationModelBase {
	private static Logger log = Logger.getLogger(KeyImporterPm.class);
	private static final String BROWSE_FOLDER = "KeyImporterPm.BROWSE_FOLDER";
	private static final String[] EXTENSIONS = new String[] { "asc", "bpg" };

	@Autowired
	private ConfigPairs configPairs;

	@Autowired
	@Resource(name = "keyFilesOperations")
	private KeyFilesOperations<KeyData> keyFilesOperations;
	@Autowired
	@Resource(name = "keyRingService")
	private KeyRingService<KeyData> keyRingService;
	private KeyImporterHost host;

	private ModelTableProperty<Key<KeyData>> keys;
	private MultipleFilesChooserDialog sourceFileChooser;
	private Comparator<Key<KeyData>> keySorterByNameAsc = new ComparatorKeyByNameImpl<KeyData>();

	public boolean init(KeyImporterHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		initModelProperties();

		File[] filesToLoad = null;
		if ((filesToLoad = getSourceFileChooser().askUserForMultipleFiles()) == null) {
			return false;
		}
		if (!loadKey(filesToLoad)) {
			return false;
		}

		return true;
	}

	private void initModelProperties() {
		actionDoImport.setEnabled(false);
		keys = new ModelTableProperty<>(this, new ArrayList<>(), "keys", new KeysTableModel());
	}

	@Override
	public void registerView(View<?> view) {
		super.registerView(view);
		Preconditions.checkState(host != null);
	}

	public MultipleFilesChooserDialog getSourceFileChooser() {
		if (sourceFileChooser == null) {
			sourceFileChooser = new MultipleFilesChooserDialog(findRegisteredWindowIfAny(), configPairs,
					BROWSE_FOLDER) {
				@Override
				protected void doFileChooserPostConstruct(JFileChooser ofd) {
					super.doFileChooserPostConstruct(ofd);
					ofd.setDialogTitle(Messages.get("action.importKey"));

					ofd.setAcceptAllFileFilterUsed(false);
					ofd.addChoosableFileFilter(keyFilesFilter);
					ofd.addChoosableFileFilter(ofd.getAcceptAllFileFilter());
					ofd.setFileFilter(ofd.getChoosableFileFilters()[0]);
				}

				private FileFilter keyFilesFilter = new FileFilter() {
					@Override
					public boolean accept(File f) {
						if (f.isDirectory() || !f.isFile()) {
							return true;
						}
						if (!isExtension(f.getName(), EXTENSIONS)) {
							return false;
						}

						// NOTE: Although it gives best results -- I have a
						// slight concern that this might be too heavy operation
						// to perform thorough -- check for each file
						// contents. My hope is that since we're checking only
						// key files it shouldn't be a problem. Non-key files
						// with same xtensions will not take a long time to fail
						try {
							Key<KeyData> readKey = keyFilesOperations.readKeyFromFile(f.getAbsolutePath());
							Preconditions.checkState(readKey != null, "Key wasn't parsed");
							return !keyRingService.isKeyAlreadyAdded(readKey);
						} catch (Throwable t) {
							// in this case it's not an issue. So it's debug
							// level
							log.debug("File is not accepte for file chooser becasue was not able to read it as a key",
									t);
						}

						return false;
					}

					private boolean isExtension(String fileName, String[] extensions) {
						String extension = FilenameUtils.getExtension(fileName);
						if (!StringUtils.hasText(extension)) {
							return false;
						}

						for (String ext : extensions) {
							if (ext.equalsIgnoreCase(extension)) {
								return true;
							}
						}
						return false;
					}

					@Override
					public String getDescription() {
						return "Key files (.asc, .bpg)";
					}
				};
			};
		}
		return sourceFileChooser;
	}

	private boolean loadKey(File[] filesToLoad) {
		try {
			Map<String, Throwable> exceptions = new HashMap<>();
			List<Key<KeyData>> loadedKeys = loadKeysSafe(filesToLoad, exceptions);

			if (exceptions.size() > 0) {
				String msg = buildSummaryMessage("error.keysLoadedStatistics", loadedKeys.size(), exceptions);
				EntryPoint.showMessageBox(null, msg, Messages.get("term.attention"), JOptionPane.ERROR_MESSAGE);
			}

			if (loadedKeys.size() > 0) {
				loadedKeys.sort(keySorterByNameAsc);
				keys.getList().addAll(loadedKeys);
				actionDoImport.setEnabled(true);
				return true;
			}
		} catch (Throwable t) {
			EntryPoint.reportExceptionToUser("exception.failedToReadKey", t);
		}
		return false;
	}

	private List<Key<KeyData>> loadKeysSafe(File[] filesToLoad, Map<String, Throwable> exceptions) {
		List<Key<KeyData>> ret = new ArrayList<>(filesToLoad.length);
		for (File keyFile : filesToLoad) {
			try {
				Key<KeyData> key = keyFilesOperations.readKeyFromFile(keyFile.getAbsolutePath());
				ret.add(key);
			} catch (Throwable t) {
				log.warn("Failed to read key file", t);
				exceptions.put(keyFile.toString(), t);
			}
		}
		return ret;
	}

	private String buildSummaryMessage(String messageCode, int successCount, Map<String, Throwable> exceptions) {
		StringBuilder sb = new StringBuilder();
		sb.append(text(messageCode, successCount, exceptions.size()));
		sb.append("\n");

		for (Entry<String, Throwable> exc : exceptions.entrySet()) {
			sb.append("\n");
			sb.append(exc.getKey());
			sb.append("\n");
			sb.append(ConsoleExceptionUtils.getAllMessages(exc.getValue()));
			sb.append("\n");
		}
		return sb.toString();
	}

	@SuppressWarnings("serial")
	private Action actionDoImport = new LocalizedAction("action.import") {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Preconditions.checkState(keys.getList().size() > 0, "No keys loaded");

				Map<String, Throwable> exceptions = new HashMap<>();
				int loadedCount = importKeysSafe(exceptions);

				if (exceptions.size() > 0) {
					String msg = buildSummaryMessage("error.keysImportStatistics", loadedCount, exceptions);
					EntryPoint.showMessageBox(null, msg, Messages.get("term.attention"), JOptionPane.ERROR_MESSAGE);
				}

				host.handleImporterFinished();
			} catch (Throwable t) {
				log.error("Failed to import keys", t);
				EntryPoint.reportExceptionToUser("exception.failedToImportPgpKey", t);
				return;
			}
		}

		private int importKeysSafe(Map<String, Throwable> exceptions) {
			int loadedCount = 0;
			for (Key<KeyData> key : keys.getList()) {
				try {
					keyRingService.addKey(key);
					loadedCount++;
				} catch (Throwable t) {
					log.warn("Failed to import key " + key.getKeyInfo().getUser(), t);
					exceptions.put(key.getKeyInfo().getUser(), t);
				}
			}
			return loadedCount;
		}
	};

	@SuppressWarnings("serial")
	private Action actionCancel = new LocalizedAction("action.cancel") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleImporterFinished();
		}
	};

	private KeysTablePm keysTablePm = new KeysTablePm() {
		@Override
		public ModelTablePropertyAccessor<Key<KeyData>> getKeys() {
			return keys.getModelTablePropertyAccessor();
		}
	};

	protected Action getActionCancel() {
		return actionCancel;
	}

	protected Action getActionDoImport() {
		return actionDoImport;
	}

	public KeysTablePm getKeysTablePm() {
		return keysTablePm;
	}

}
