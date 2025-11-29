package ru.skarpushin.swingpm.modelprops.lists;

import javax.swing.ComboBoxModel;

public interface ModelSelInComboBoxPropertyAccessor<E>
    extends ModelListPropertyAccessor<E>, ComboBoxModel<E> {}
