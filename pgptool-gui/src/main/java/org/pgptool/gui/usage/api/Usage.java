package org.pgptool.gui.usage.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This impl simply writes everything to the log file which is already used throughout the
 * application. This will not allow application to analyze past user activity, but it will help in
 * cases they have issues with application and I request them to send me details.
 *
 * @author sergeyk
 */
public class Usage implements UsageLogger {
  private static final Logger log = LoggerFactory.getLogger("USAGE");

  private final Gson gson;

  public Usage() {
    GsonBuilder b = new GsonBuilder();
    b.registerTypeAdapter(
        Serializable.class, new SubclassAwareJsonSerializationAdapter<>(Serializable.class));
    gson = b.create();
  }

  @Override
  public <T extends Serializable> void write(T usageEvent) {
    try {
      log.info(gson.toJson(new UsageEvent(usageEvent)));
    } catch (Throwable t) {
      log.warn("Failed to serialize: {}", usageEvent, t);
    }
  }
}
