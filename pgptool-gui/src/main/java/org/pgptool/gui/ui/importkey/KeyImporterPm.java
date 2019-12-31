/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
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
 ******************************************************************************/
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
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.ui.keyslist.ComparatorKeyByNameImpl;
import org.pgptool.gui.ui.keyslist.KeysTableModel;
import org.pgptool.gui.ui.keyslist.KeysTablePm;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.MultipleFilesChooserDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.EXPORT.base.LocalizedActionEx;
import ru.skarpushin.swingpm.EXPORT.base.PresentationModelBase;
import ru.skarpushin.swingpm.base.View;
import ru.skarpushin.swingpm.modelprops.table.ModelTableProperty;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;

public class KeyImporterPm extends PresentationModelBase<KeyImporterHost, List<Key>> {
	private static Logger log = Logger.getLogger(KeyImporterPm.class);
	private static final String BROWSE_FOLDER = "KeyImporterPm.BROWSE_FOLDER";
	private static final String[] EXTENSIONS = new String[] { "asc", "bpg" };

	@Autowired
	private ConfigPairs appProps;
	@Autowired
	private KeyFilesOperations keyFilesOperations;
	@Autowired
	private KeyRingService keyRingService;

	private ModelTableProperty<Key> keys;
	private MultipleFilesChooserDialog sourceFileChooser;
	private Comparator<Key> keySorterByNameAsc = new ComparatorKeyByNameImpl();

	@Override
	public boolean init(ActionEvent originAction, KeyImporterHost host, List<Key> preloadedKeys) {
		super.init(originAction, host, preloadedKeys);
		if (preloadedKeys != null) {
			initWithPreloadedKeys(preloadedKeys);
			return true;
		}
		if (!initByAskingUserToChooseFiles(originAction)) {
			return false;
		}
		return true;
	}

	private boolean initByAskingUserToChooseFiles(ActionEvent originEvent) {
		Preconditions.checkArgument(host != null);
		initModelProperties();

		File[] filesToLoad = null;
		if ((filesToLoad = getSourceFileChooser().askUserForMultipleFiles(originEvent)) == null) {
			return false;
		}
		if (!loadKey(filesToLoad)) {
			return false;
		}

		return true;
	}

	private void initWithPreloadedKeys(List<Key> keys) {
		Preconditions.checkArgument(host != null);
		Preconditions.checkArgument(!CollectionUtils.isEmpty(keys));
		initModelProperties();
		initLoadedKeys(keys);
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
			sourceFileChooser = new MultipleFilesChooserDialog(appProps, BROWSE_FOLDER) {
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
							List<Key> readKeys = keyFilesOperations.readKeysFromFile(f);
							for (Key readKey : readKeys) {
								Key existingKey = keyRingService.findKeyById(readKey.getKeyInfo().getKeyId());
								if (existingKey == null) {
									return true;
								}
								if (!existingKey.getKeyData().isCanBeUsedForDecryption()
										&& readKey.getKeyData().isCanBeUsedForDecryption()) {
									return true;
								}
							}
							return false;
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
			List<Key> loadedKeys = loadKeysSafe(filesToLoad, exceptions);

			if (exceptions.size() > 0) {
				String msg = buildSummaryMessage("error.keysLoadedStatistics", loadedKeys.size(), exceptions);
				UiUtils.messageBox(originAction, msg, Messages.get("term.attention"), JOptionPane.ERROR_MESSAGE);
			}

			if (loadedKeys.size() > 0) {
				initLoadedKeys(loadedKeys);
				return true;
			}
		} catch (Throwable t) {
			EntryPoint.reportExceptionToUser(originAction, "exception.failedToReadKey", t);
		}
		return false;
	}

	private void initLoadedKeys(List<Key> loadedKeys) {
		loadedKeys.sort(keySorterByNameAsc);
		keys.getList().addAll(loadedKeys);
		actionDoImport.setEnabled(true);
	}

	private List<Key> loadKeysSafe(File[] filesToLoad, Map<String, Throwable> exceptions) {
		List<Key> ret = new ArrayList<>(filesToLoad.length);
		for (File keyFile : filesToLoad) {
			try {
				List<Key> readKeys = keyFilesOperations.readKeysFromFile(keyFile);
				ret.addAll(readKeys);
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
	private Action actionDoImport = new LocalizedActionEx("action.import", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			try {
				Preconditions.checkState(keys.getList().size() > 0, "No keys loaded");

				Map<String, Throwable> exceptions = new HashMap<>();
				int loadedCount = importKeysSafe(exceptions);

				if (exceptions.size() > 0) {
					String msg = buildSummaryMessage("error.keysImportStatistics", loadedCount, exceptions);
					UiUtils.messageBox(e, msg, Messages.get("term.attention"), JOptionPane.ERROR_MESSAGE);
				}

				host.handleImporterFinished();
			} catch (Throwable t) {
				log.error("Failed to import keys", t);
				EntryPoint.reportExceptionToUser(e, "exception.failedToImportPgpKey", t);
				return;
			}
		}

		private int importKeysSafe(Map<String, Throwable> exceptions) {
			int loadedCount = 0;
			for (Key key : keys.getList()) {
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
	private Action actionCancel = new LocalizedActionEx("action.cancel", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			host.handleImporterFinished();
		}
	};

	private KeysTablePm keysTablePm = new KeysTablePm() {
		@Override
		public ModelTablePropertyAccessor<Key> getKeys() {
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
