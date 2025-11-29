package ru.skarpushin.swingpm.base;

import java.awt.Container;

public interface View<TPM> extends Detachable {
  TPM getPm();

  void setPm(TPM pm);

  /**
   * Render to specific container
   *
   * @param target assuming it's null, Containrt or Window or Frame
   * @param constraints constaraints for case when view is a component added to panel or other
   *     component
   */
  void renderTo(Container target, Object constraints);

  void renderTo(Container target);

  void unrender();
}
