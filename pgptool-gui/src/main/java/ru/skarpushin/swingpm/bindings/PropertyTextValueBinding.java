package ru.skarpushin.swingpm.bindings;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;

public class PropertyTextValueBinding implements Binding, PropertyChangeListener, DocumentListener {
  private static final Logger log = LoggerFactory.getLogger(PropertyTextValueBinding.class);

  private final ModelPropertyAccessor<String> property;
  private final Document textDocument;
  private boolean pausePropagation = false;

  public PropertyTextValueBinding(ModelPropertyAccessor<String> property, Document textDocument) {
    this.property = property;
    this.textDocument = textDocument;

    try {
      String str = property.getValue() == null ? "" : property.getValue();

      textDocument.remove(0, textDocument.getLength());
      textDocument.insertString(0, str, null);
    } catch (BadLocationException e) {
      log.warn(
          "Failed to re-insert string to force selection. Property name = {}",
          property.getPropertyName(),
          e);
    }

    property.addPropertyChangeListener(this);
    textDocument.addDocumentListener(this);
  }

  @Override
  public boolean isBound() {
    return textDocument != null;
  }

  @Override
  public void unbind() {
    property.removePropertyChangeListener(this);
    textDocument.removeDocumentListener(this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    try {
      if (getTextFromView().equals(evt.getNewValue())) {
        return;
      }

      pausePropagation = true;
      textDocument.remove(0, textDocument.getLength());
      pausePropagation = false;
      textDocument.insertString(0, (String) evt.getNewValue(), null);
    } catch (Throwable exc) {
      throw new RuntimeException(
          "Failed to propagate text property change from model to view", exc);
    }
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    propagateValueFromViewToModel();
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    propagateValueFromViewToModel();
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    propagateValueFromViewToModel();
  }

  private void propagateValueFromViewToModel() {
    if (pausePropagation) {
      return;
    }
    try {
      property.setValue(getTextFromView());
    } catch (Throwable exc) {
      throw new RuntimeException(
          "Failed to propagate text property change from view to model", exc);
    }
  }

  private String getTextFromView() throws BadLocationException {
    return textDocument.getText(0, textDocument.getLength());
  }
}
