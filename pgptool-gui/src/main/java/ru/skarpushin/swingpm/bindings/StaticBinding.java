package ru.skarpushin.swingpm.bindings;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.lang.reflect.Method;
import javax.swing.text.Document;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.ModelPropertyReader;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReflectionImpl;

/**
 * Class which provides static binding facilities to the properties
 *
 * @author sergeyk
 */
public class StaticBinding {
  /**
   * @deprecated avoid using this approach cause it uses hard coded literal of target method name.
   *     Use {@link #registerOnChangeHandler(ModelPropertyReader, TypedPropertyChangeListener)}
   *     instead
   */
  @Deprecated
  public static Binding registerOnChangeHandler(
      ModelPropertyAccessor<?> property, Object targetObject, String methodName) {
    Preconditions.checkNotNull(property);
    Preconditions.checkNotNull(targetObject);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(methodName));

    try {
      Method handlerMethod = getHandlerMethod(targetObject, methodName);
      return new PropertyChangeHandlerReflectionBinding(property, targetObject, handlerMethod);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to register change handler", t);
    }
  }

  public static <T> Binding registerOnChangeHandler(
      ModelPropertyReader<T> property, TypedPropertyChangeListener<T> listener) {
    Preconditions.checkNotNull(property);
    Preconditions.checkNotNull(listener);

    try {
      return new PropertyChangeHandlerBinding<>(property, listener);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to register change handler", t);
    }
  }

  private static Method getHandlerMethod(Object targetObject, String methodName) {
    try {
      for (Method method : targetObject.getClass().getMethods()) {
        if (!method.getName().equals(methodName)) {
          continue;
        }

        if (method.getParameterTypes().length != 3) {
          continue;
        }

        return method;
      }

      throw new RuntimeException(
          "No method of class "
              + targetObject.getClass()
              + " named as "
              + methodName
              + " and has 3 parameters");
    } catch (Throwable e) {
      throw new RuntimeException("Failed to locate handler method", e);
    }
  }

  /**
   * Register one way property propagation. If property will change, it's value will be propagated
   * to target
   */
  public static <E> Binding registerPropertyValuePropagation(
      ModelPropertyReader<E> property, Object targetObject, String targetProperty) {
    Preconditions.checkNotNull(property);
    Preconditions.checkNotNull(targetObject);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(targetProperty));

    try {
      return new PropertyValuePropagationBinding(
          property, new ValueAdapterReflectionImpl<E>(targetObject, targetProperty));
    } catch (Throwable t) {
      throw new RuntimeException(
          "Failed to register " + property + " property change propagation", t);
    }
  }

  public static Binding registerTextPropertyBinding(
      ModelPropertyAccessor<String> property, Document textDocument) {
    Preconditions.checkNotNull(property);
    Preconditions.checkNotNull(textDocument);

    try {
      return new PropertyTextValueBinding(property, textDocument);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to register " + property + " two-way property binding", t);
    }
  }
}
