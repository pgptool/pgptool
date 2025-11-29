package ru.skarpushin.swingpm.tools;

import com.google.common.base.Preconditions;
import ru.skarpushin.swingpm.bindings.BindingContextFactory;
import ru.skarpushin.swingpm.bindings.BindingContextFactoryDefaultImpl;
import ru.skarpushin.swingpm.tools.i18n.MessagesProvider;
import ru.skarpushin.swingpm.tools.i18n.MessagesProviderNoOpImpl;

/**
 * Use this class to integrate SwingPm into your application.
 *
 * @author sergeyk
 */
public class SwingPmSettings {
  private static MessagesProvider messages = new MessagesProviderNoOpImpl();
  private static BindingContextFactory bindingContextFactory =
      new BindingContextFactoryDefaultImpl();

  public static MessagesProvider getMessages() {
    return messages;
  }

  public static void setMessages(MessagesProvider messages) {
    Preconditions.checkArgument(messages != null, "Message Provider required");
    SwingPmSettings.messages = messages;
  }

  public static BindingContextFactory getBindingContextFactory() {
    return bindingContextFactory;
  }

  public static void setBindingContextFactory(BindingContextFactory bindingContextFactory) {
    Preconditions.checkArgument(bindingContextFactory != null, "BindingContextFactory required");
    SwingPmSettings.bindingContextFactory = bindingContextFactory;
  }
}
