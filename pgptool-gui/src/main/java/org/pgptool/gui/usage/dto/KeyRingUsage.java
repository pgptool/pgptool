package org.pgptool.gui.usage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyInfo;

public class KeyRingUsage implements Serializable {
  @Serial private static final long serialVersionUID = 658399021937944608L;

  private List<KeyInfo> keys;

  public KeyRingUsage() {}

  public KeyRingUsage(List<Key> keyRing) {
    keys = keyRing.stream().map(Key::getKeyInfo).collect(Collectors.toList());
  }

  public List<KeyInfo> getKeys() {
    return keys;
  }

  public void setKeys(List<KeyInfo> keys) {
    this.keys = keys;
  }
}
