package ru.skarpushin.swingpm.modelprops.virtualtable;

import org.summerb.utils.easycrud.api.dto.PagerParams;
import org.summerb.utils.easycrud.api.dto.PaginatedList;
import ru.skarpushin.swingpm.collections.FilterPredicate;

/**
 * Adapter for loading additional “pages” of data on demand. Provided to AsyncDataLoader.
 *
 * <p>Note that impl must respect other data filtering criteria specific to usage situation.
 *
 * <p>Note base interface FilterPredicate - impl must be able to check if data satisfy context
 * filtering parameters.
 *
 * @see AsyncDataLoader
 */
public interface VirtualTableDataSource<E> extends FilterPredicate<E> {
  PaginatedList<E> loadData(PagerParams pagerParams);
}
