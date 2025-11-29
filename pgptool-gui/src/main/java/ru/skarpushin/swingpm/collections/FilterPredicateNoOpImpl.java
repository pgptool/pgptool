package ru.skarpushin.swingpm.collections;

/**
 * Does nothing except accept all optionsAccessor
 *
 * @author sergey.karpushin
 * @param <T>
 */
public class FilterPredicateNoOpImpl<T> implements FilterPredicate<T> {
  @Override
  public boolean isSuitable(T subject) {
    return true;
  }
}
