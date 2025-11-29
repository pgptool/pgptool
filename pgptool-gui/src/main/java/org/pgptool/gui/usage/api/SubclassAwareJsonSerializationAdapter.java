package org.pgptool.gui.usage.api;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class SubclassAwareJsonSerializationAdapter<T>
    implements JsonSerializer<T>, JsonDeserializer<T> {
  // private static Logger log =
  // Logger.getLogger(SubclassAwareJsonSerializationAdapter.class);
  private static final String CLASSNAME = "c";
  private static final String INSTANCE = "i";

  /**
   * We need that to avoid infinite recursion (stack overflow) when working with objects of class we
   * have adapter for. If we reuse context to work with instances - it will fail. So we need
   * separata Gson which is unaware of this custom SubclassAwareJsonSerializationAdapter
   */
  private final Gson simpleGson = new Gson();

  private final Class<T> abstractBaseClass;

  public SubclassAwareJsonSerializationAdapter(Class<T> abstractBaseClass) {
    this.abstractBaseClass = abstractBaseClass;
  }

  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject retValue = new JsonObject();
    String className = src.getClass().getSimpleName().replace("Usage", "");
    retValue.addProperty(CLASSNAME, className);

    JsonElement elem;
    if (abstractBaseClass.equals(src.getClass())) {
      // Avoid infinite recursion
      elem = simpleGson.toJsonTree(src);
    } else {
      elem = context.serialize(src);
    }

    retValue.add(INSTANCE, elem);
    return retValue;
  }

  @Override
  public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    throw new IllegalStateException("Not implemented");
  }
}
