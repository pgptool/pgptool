package org.pgptool.gui.usage.dto;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyInfo;

public class KeyRingUsage implements Serializable {
  private static final long serialVersionUID = 658399021937944608L;

  private List<KeyInfo> keys;

  public KeyRingUsage() {}

  public KeyRingUsage(List<Key> keyRing) {
    keys = keyRing.stream().map(x -> x.getKeyInfo()).collect(Collectors.toList());
  }

  public List<KeyInfo> getKeys() {
    return keys;
  }

  public void setKeys(List<KeyInfo> keys) {
    this.keys = keys;
  }
}
