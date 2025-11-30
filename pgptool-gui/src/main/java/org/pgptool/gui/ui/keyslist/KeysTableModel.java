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
package org.pgptool.gui.ui.keyslist;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyInfo;
import org.pgptool.gui.ui.tools.KeyInfoRendering;
import ru.skarpushin.swingpm.modelprops.table.LightweightTableModel;

public class KeysTableModel implements LightweightTableModel<Key> {
  public static final int COLUMN_USER = 0;
  public static final int COLUMN_KEY_ID = 1;
  public static final int COLUMN_ALGORITHM = 2;
  public static final int COLUMN_KEY_TYPE = 3;
  public static final int COLUMN_CREATED_ON = 4;
  public static final int COLUMN_EXPIRES_AT = 5;

  @Override
  public int getColumnCount() {
    return 6;
  }

  @Override
  public String getColumnName(int columnIndex) {
    return switch (columnIndex) {
      case COLUMN_USER -> Messages.get("term.user");
      case COLUMN_KEY_ID -> Messages.get("term.keyId");
      case COLUMN_KEY_TYPE -> Messages.get("term.keyType");
      case COLUMN_ALGORITHM -> Messages.get("term.keyAlgorithm");
      case COLUMN_CREATED_ON -> Messages.get("term.createdOn");
      case COLUMN_EXPIRES_AT -> Messages.get("term.expiresAt");
      default -> throw new IllegalArgumentException("Wrong column index: " + columnIndex);
    };
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return String.class;
  }

  @Override
  public Object getValueAt(Key r, int columnIndex) {
    if (r == null) {
      return "";
    }

    KeyInfo info = r.getKeyInfo();

    return switch (columnIndex) {
      case COLUMN_USER -> " " + info.getUser();
      case COLUMN_KEY_ID -> info.getKeyId();
      case COLUMN_KEY_TYPE -> KeyInfoRendering.keyTypeToString(info);
      case COLUMN_ALGORITHM -> info.getKeyAlgorithm();
      case COLUMN_CREATED_ON -> KeyInfoRendering.dateToString(info.getCreatedOn());
      case COLUMN_EXPIRES_AT -> KeyInfoRendering.dateToString(info.getExpiresAt());
      default -> throw new IllegalArgumentException("Wrong column index: " + columnIndex);
    };
  }
}
