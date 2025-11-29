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
package org.pgptool.gui.config.impl;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.log4j.Logger;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.tools.FileUtilsEx;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import org.summerb.utils.DtoBase;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent;

public class ConfigRepositoryImpl implements ConfigRepository, InitializingBean {
  private static final Logger log = Logger.getLogger(ConfigRepositoryImpl.class);
  private final String configsBasepath = File.separator + "configs";

  private final ConfigsBasePathResolver configsBasePathResolver;
  private final EventBus eventBus;

  public ConfigRepositoryImpl(ConfigsBasePathResolver configsBasePathResolver, EventBus eventBus) {
    this.configsBasePathResolver = configsBasePathResolver;
    this.eventBus = eventBus;
  }

  @Override
  public void afterPropertiesSet() {
    ensureAllDirsCreated();
  }

  private void ensureAllDirsCreated() {
    File configsFolder = new File(configsBasePathResolver.getConfigsBasePath() + configsBasepath);
    if (!configsFolder.exists() && !configsFolder.mkdirs()) {
      throw new RuntimeException("Failed to ensure all dirs for config files: " + configsFolder);
    }
  }

  @Override
  public <T extends DtoBase> void persist(T object) {
    persist(object, null);
  }

  @Override
  public <T extends DtoBase> void persist(T object, String clarification) {
    try {
      Preconditions.checkArgument(object != null, "Can't persist null object");
      String filename = buildFilenameForClass(object.getClass());
      filename = addClarification(filename, clarification);
      FileUtilsEx.baitAndSwitch(filename, x -> writeObject(object, x));
      eventBus.post(EntityChangedEvent.updated(object));
      log.debug("Updating config file: " + filename);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to persist object " + object, t);
    }
  }

  private String addClarification(String filename, String clarification) {
    if (StringUtils.hasText(clarification)) {
      filename += "." + clarification;
    }
    return filename;
  }

  private String buildFilenameForClass(Class<?> clazz) {
    return configsBasePathResolver.getConfigsBasePath()
        + configsBasepath
        + File.separator
        + clazz.getSimpleName();
  }

  @Override
  public <T extends DtoBase> T read(Class<T> clazz) {
    return read(clazz, null);
  }

  @Override
  public <T extends DtoBase> T read(Class<T> clazz, String clarification) {
    try {
      Preconditions.checkArgument(clazz != null, "Class must be provided");
      String filename = buildFilenameForClass(clazz);
      filename = addClarification(filename, clarification);
      if (!new File(filename).exists()) {
        return null;
      }
      return readObject(filename);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to read object of class " + clazz, t);
    }
  }

  @Override
  public <T extends DtoBase> T readOrConstruct(Class<T> clazz) {
    return readOrConstruct(clazz, null);
  }

  @Override
  public <T extends DtoBase> T readOrConstruct(Class<T> clazz, String clarification) {
    T result = read(clazz, clarification);
    if (result == null) {
      try {
        result = clazz.getConstructor().newInstance();
      } catch (Throwable t) {
        throw new RuntimeException("Failed to create new instance of " + clazz, t);
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  public static <T extends DtoBase> T readObject(String sourceFile) {
    ObjectInputStream ois = null;
    try {
      File file = new File(sourceFile);
      if (!file.exists()) {
        return null;
      }

      FileInputStream fis = new FileInputStream(file);
      ois = new ObjectInputStream(fis);
      T ret = (T) ois.readObject();
      ois.close();
      return ret;
    } catch (Throwable t) {
      log.warn("Failed to read " + sourceFile, t);
      return null;
    } finally {
      safeClose(ois);
    }
  }

  public static void safeClose(ObjectInputStream fis) {
    if (fis != null) {
      try {
        fis.close();
      } catch (Throwable t) {
        // don't care
      }
    }
  }

  public static void writeObject(Object o, String destinationFile) {
    ObjectOutputStream oos = null;
    try {
      File file = new File(destinationFile);
      if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
        throw new RuntimeException("Failed to create all parent directories");
      }

      FileOutputStream fout = new FileOutputStream(file);
      oos = new ObjectOutputStream(fout);
      oos.writeObject(o);
      oos.flush();
      oos.close();
      fout.close();
    } catch (Throwable t) {
      throw new RuntimeException("Failed to write config: " + destinationFile, t);
    } finally {
      safeClose(oos);
    }
  }

  public static void safeClose(ObjectOutputStream fis) {
    if (fis != null) {
      try {
        fis.close();
      } catch (Throwable t) {
        // don't care
      }
    }
  }
}
