package ru.skarpushin.swingpm.tools.sglayout;

import com.google.common.base.Preconditions;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple but yet smart enough grid layout to not brake layout because of JLabel and other
 * components
 *
 * @author sergey.karpushin
 */
public class SgLayout implements LayoutManager2 {
  public static final int DEFAULT_ROW_HEIGHT = 14;
  public static final int DEFAULT_COL_WIDTH = 25;
  public static final int SIZE_TYPE_CONSTANT = 1;
  public static final int SIZE_TYPE_WEIGHTED = 2;
  public static final int SIZE_TYPE_ASKCOMPONENT = 3;
  public static final SizeResolver preferredWidth = c -> c.getPreferredSize().width;
  public static final SizeResolver preferredHeight = c -> c.getPreferredSize().height;
  public static final SizeResolver minimumWidth = c -> c.getMinimumSize().width;
  public static final SizeResolver minimumHeight = c -> c.getMinimumSize().height;
  private static final Logger log = LoggerFactory.getLogger(SgLayout.class);
  private final int rows;
  private final int cols;
  private final int hgap;
  private final int vgap;
  private final Map<Component, SmartGridLayoutConstraints> components = new HashMap<>();

  private int[] rowsSizes;
  private int[] rowsSizesTypes;
  private int[] indexesOfWeightedRows;
  private int[] baselineRowSizes;
  private int[] rowsSizesEffective;

  private int[] colsSizes;
  private int[] colsSizesTypes;
  private int[] indexesOfWeightedCols;
  private int[] baselineColSizes;
  private int[] colsSizesEffective;

  /** Name might be useful for debugging purposes */
  private String name;

  public SgLayout(int cols, int rows, int hgap, int vgap) {
    Preconditions.checkArgument(rows > 0, "rows must be > 0");
    Preconditions.checkArgument(cols > 0, "cols must be > 0");
    Preconditions.checkArgument(hgap >= 0, "hgap must be >= 0");
    Preconditions.checkArgument(vgap >= 0, "vgap must be >= 0");

    this.rows = rows;
    this.cols = cols;
    initSizeArrays();

    this.hgap = hgap;
    this.vgap = vgap;
  }

  public static SmartGridLayoutConstraints c(int col, int row) {
    return new SmartGridLayoutConstraints(col, row);
  }

  public static SmartGridLayoutConstraints c(int col, int row, int colspan, int rowspan) {
    return new SmartGridLayoutConstraints(col, row, colspan, rowspan);
  }

  public void setRowSize(int row, int size, int sizeType) {
    Preconditions.checkArgument(0 <= row && row < rows, "Invalid row idx");
    Preconditions.checkArgument(size > 0, "Invalid size");
    Preconditions.checkArgument(
        SIZE_TYPE_CONSTANT <= sizeType && sizeType <= SIZE_TYPE_ASKCOMPONENT, "Invalid size type");

    rowsSizes[row] = size;
    rowsSizesTypes[row] = sizeType;
    indexesOfWeightedRows = null;
  }

  public void setColSize(int col, int size, int sizeType) {
    Preconditions.checkArgument(0 <= col && col < cols, "Invalid col idx");
    Preconditions.checkArgument(size > 0, "Invalid size");
    Preconditions.checkArgument(
        SIZE_TYPE_CONSTANT <= sizeType && sizeType <= SIZE_TYPE_ASKCOMPONENT, "Invalid size type");

    colsSizes[col] = size;
    colsSizesTypes[col] = sizeType;
    indexesOfWeightedCols = null;
  }

  /**
   * Calculate effective cols and rows sizes which has dynamic sizes.
   *
   * @param dynaSpace space which is dynamically available and is to be distributed between
   *     dynamically sized components
   */
  private void calculateWeightedSizes(
      int dynaSpace, int[] indexesOfWeighted, int[] weights, int[] baselineSizes, int[] results) {

    // Make sure static sizes are copied
    if (indexesOfWeighted.length < results.length) {
      System.arraycopy(baselineSizes, 0, results, 0, results.length);
    }

    Set<Integer> minSizeRows = null;
    boolean recalc;

    do {
      recalc = false;
      double dynaSpaceD = dynaSpace;
      double weightsSumm = 0;
      for (int i : indexesOfWeighted) {
        if (minSizeRows != null && minSizeRows.contains(i)) {
          continue;
        }
        weightsSumm += weights[i];
      }

      // Identify all rows which has to stay at minimum size even if it violates weights
      for (int i : indexesOfWeighted) {
        if (minSizeRows != null && minSizeRows.contains(i)) {
          continue;
        }

        results[i] = (int) (dynaSpaceD * ((double) weights[i]) / weightsSumm);
        if (results[i] < baselineSizes[i]) {
          results[i] = baselineSizes[i];
          dynaSpace -= baselineSizes[i];
          recalc = true;
          if (minSizeRows == null) {
            minSizeRows = new HashSet<>();
          }
          minSizeRows.add(i);
        }
      }

    } while (recalc);
  }

  public SmartGridLayoutConstraints cs(int col, int row) {
    return new SmartGridLayoutConstraints(col, row);
  }

  public SmartGridLayoutConstraints cs(int col, int row, int colspan, int rowspan) {
    return new SmartGridLayoutConstraints(col, row, colspan, rowspan);
  }

  private void initSizeArrays() {
    rowsSizes = new int[rows];
    rowsSizesTypes = new int[rows];
    rowsSizesEffective = new int[rows];
    baselineRowSizes = new int[rows];

    colsSizes = new int[cols];
    colsSizesTypes = new int[cols];
    colsSizesEffective = new int[cols];
    baselineColSizes = new int[cols];

    for (int i = 0; i < rows; i++) {
      rowsSizes[i] = DEFAULT_ROW_HEIGHT;
      rowsSizesTypes[i] = SIZE_TYPE_ASKCOMPONENT;
    }

    for (int i = 0; i < cols; i++) {
      colsSizes[i] = DEFAULT_COL_WIDTH;
      colsSizesTypes[i] = SIZE_TYPE_WEIGHTED;
    }
  }

  @Override
  public void addLayoutComponent(String name, Component comp) {
    log.error("Operation NOT supported");
    throw new IllegalStateException("Operation not supported");
  }

  @Override
  public void addLayoutComponent(Component comp, Object constraints) {
    synchronized (comp.getTreeLock()) {
      Preconditions.checkArgument(
          constraints instanceof SmartGridLayoutConstraints,
          "Constraints must not be null and instanceof SmartGridLayoutConstraints");

      SmartGridLayoutConstraints c = (SmartGridLayoutConstraints) constraints;

      Preconditions.checkArgument(
          c.col + c.colspan - 1 < cols, "Coordinates intersects with grid bounds by cols");
      Preconditions.checkArgument(
          c.row + c.rowspan - 1 < rows, "Coordinates intersects with grid bounds by rows");

      Component offending = findComponentWithinBounds(c);
      Preconditions.checkArgument(
          offending == null,
          "can't place new component to intersect with existing one: " + offending);

      // Add component to map
      components.put(comp, c);
    }
  }

  private Component findComponentWithinBounds(SmartGridLayoutConstraints c1) {
    Rectangle r1 = new Rectangle(c1.col, c1.row, c1.colspan, c1.rowspan);

    for (Entry<Component, SmartGridLayoutConstraints> entry : components.entrySet()) {
      SmartGridLayoutConstraints c2 = entry.getValue();
      Rectangle r2 = new Rectangle(c2.col, c2.row, c2.colspan, c2.rowspan);

      if (r1.intersects(r2)) {
        log.warn("Intersection for components: new {} and existing {}", r1, r2);
        return entry.getKey();
      }
    }

    return null;
  }

  private Component findComponentAt(int col, int row) {
    for (Entry<Component, SmartGridLayoutConstraints> entry : components.entrySet()) {
      SmartGridLayoutConstraints c = entry.getValue();
      if ((c.col <= col && col <= c.col + c.colspan - 1)
          && (c.row <= row && row <= c.row + c.rowspan - 1)) {
        return entry.getKey();
      }
    }

    return null;
  }

  @Override
  public void removeLayoutComponent(Component comp) {
    synchronized (comp.getTreeLock()) {
      components.remove(comp);
    }
  }

  @Override
  public Dimension minimumLayoutSize(Container parent) {
    log.debug("ENTERING minimumLayoutSize");

    synchronized (parent.getTreeLock()) {
      Insets parentInsets = parent.getInsets();

      calcBaselineColSizes(minimumWidth);
      calcBaselineRowSizes(minimumHeight);

      Dimension ret = new Dimension(sum(baselineColSizes, hgap), sum(baselineRowSizes, vgap));

      if (log.isDebugEnabled()) {
        log.debug("RETURN minimumLayoutSize {}", ret);
      }
      return add(ret, parentInsets);
    }
  }

  private int sum(int[] ints, int gap) {
    int ret = 0;
    for (int i : ints) {
      ret += i;
    }
    if (gap > 0) {
      ret += gap * (ints.length - 1);
    }
    return ret;
  }

  private int sumNonWeighted(int[] ints, int gap, int[] sizeTypes) {
    int ret = 0;
    for (int i = 0; i < ints.length; i++) {
      if (sizeTypes[i] != SIZE_TYPE_WEIGHTED) {
        ret += ints[i];
      }
    }
    if (gap > 0) {
      ret += gap * (ints.length - 1);
    }
    return ret;
  }

  @Override
  public Dimension preferredLayoutSize(Container parent) {
    log.debug("\n\n\nENTERING preferredLayoutSize");

    synchronized (parent.getTreeLock()) {
      // See how much space we have for our rendering
      Insets parentInsets = parent.getInsets();
      int availWidth = parent.getWidth() - (parentInsets.left + parentInsets.right);
      int availHeight = parent.getHeight() - (parentInsets.top + parentInsets.bottom);

      // See how much space is for constantly sized components and which
      // for dynamically
      calcBaselineColSizes(preferredWidth);
      updateComponentsWidths(baselineColSizes);
      calcBaselineRowSizes(preferredHeight);
      int prefWidth = sum(baselineColSizes, hgap);
      int prefHeight = sum(baselineRowSizes, vgap);
      if (availWidth < prefWidth || availHeight < prefHeight) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Available size {} x {} is LESS than preferred: {} x {}",
              availWidth,
              availHeight,
              prefWidth,
              prefHeight);
        }
        // return minimumLayoutSize(parent);
      }
      Dimension ret = new Dimension(prefWidth, prefHeight);
      if (log.isDebugEnabled()) {
        log.debug("RETURN preferredLayoutSize: {}", ret);
      }
      return add(ret, parentInsets);
    }
  }

  private Dimension add(Dimension ret, Insets ins) {
    return new Dimension(ret.width + ins.left + ins.right, ret.height + ins.top + ins.bottom);
  }

  @Override
  public Dimension maximumLayoutSize(Container target) {
    return new Dimension(10000, 10000);
  }

  @Override
  public void invalidateLayout(Container target) {
    // do nothing, not needed to be implemented
  }

  @Override
  public void layoutContainer(Container parent) {
    log.debug("\n\n\nENTERING layoutContainer");
    synchronized (parent.getTreeLock()) {
      // See how much space we have for our rendering
      Insets parentInsets = parent.getInsets();

      // Process columns sizes
      int availWidth = parent.getWidth() - (parentInsets.left + parentInsets.right);
      calculateEffectiveColSizesForLayout(availWidth);

      // Process rows sizes
      int availHeight = parent.getHeight() - (parentInsets.top + parentInsets.bottom);
      calculateEffectiveRowsSizesForLayout(availHeight);

      // Place controls to their places
      positionComponents(parentInsets);
    }
  }

  private void calculateEffectiveRowsSizesForLayout(int availHeight) {
    calcBaselineRowSizes(preferredHeight);
    int minPreferred = sum(baselineRowSizes, vgap);
    if (availHeight < minPreferred) {
      if (log.isDebugEnabled()) {
        log.debug("AvailHeight {} less than preferred {}", availHeight, minPreferred);
      }
      return;
    }
    int constHeight = sumNonWeighted(baselineRowSizes, vgap, rowsSizesTypes);
    int dynaSpace = availHeight - constHeight;
    if (dynaSpace > 0) {
      if (indexesOfWeightedRows == null) {
        indexesOfWeightedRows = initIndexesOfWeightedItems(rowsSizesTypes);
      }
      calculateWeightedSizes(
          dynaSpace, indexesOfWeightedRows, rowsSizes, baselineRowSizes, rowsSizesEffective);
    } else {
      System.arraycopy(baselineRowSizes, 0, rowsSizesEffective, 0, rowsSizesEffective.length);
    }
  }

  private void calculateEffectiveColSizesForLayout(int availWidth) {
    calcBaselineColSizes(preferredWidth);
    int minPreferred = sum(baselineColSizes, hgap);
    if (availWidth < minPreferred) {
      if (log.isDebugEnabled()) {
        log.debug("AvailWidth {} less than preferred {}", availWidth, minPreferred);
      }
      return;
    }
    int constWidth = sumNonWeighted(baselineColSizes, hgap, colsSizesTypes);
    int dynaSpace = availWidth - constWidth;
    if (dynaSpace > 0) {
      if (indexesOfWeightedCols == null) {
        indexesOfWeightedCols = initIndexesOfWeightedItems(colsSizesTypes);
      }
      calculateWeightedSizes(
          dynaSpace, indexesOfWeightedCols, colsSizes, baselineColSizes, colsSizesEffective);
    } else {
      System.arraycopy(baselineColSizes, 0, colsSizesEffective, 0, colsSizesEffective.length);
    }

    updateComponentsWidths(colsSizesEffective);
  }

  private int[] initIndexesOfWeightedItems(int[] sizesTypes) {
    int[] ret = new int[sizesTypes.length];
    int count = 0;
    for (int i = 0; i < sizesTypes.length; i++) {
      if (sizesTypes[i] == SIZE_TYPE_WEIGHTED) {
        ret[count++] = i;
      }
    }
    if (count == 0) {
      return new int[0];
    } else if (count < sizesTypes.length) {
      return Arrays.copyOf(ret, count);
    } else {
      return ret;
    }
  }

  private void positionComponents(Insets parentInsets) {
    for (Entry<Component, SmartGridLayoutConstraints> entry : components.entrySet()) {
      SmartGridLayoutConstraints c = entry.getValue();

      int left = parentInsets.left;
      for (int idxCol = 0; idxCol < c.col; idxCol++) {
        left += colsSizesEffective[idxCol];
        left += hgap;
      }

      int top = parentInsets.top;
      for (int idxRow = 0; idxRow < c.row; idxRow++) {
        top += rowsSizesEffective[idxRow];
        top += vgap;
      }

      int width = 0;
      for (int idxCol = c.col; idxCol < c.colspan + c.col; idxCol++) {
        width += colsSizesEffective[idxCol];
        if (idxCol > c.col) {
          width += hgap;
        }
      }

      int height = 0;
      for (int idxRow = c.row; idxRow < c.rowspan + c.row; idxRow++) {
        height += rowsSizesEffective[idxRow];
        if (idxRow > c.row) {
          height += vgap;
        }
      }

      Component comp = entry.getKey();
      comp.setBounds(left, top, width, height);
    }
  }

  private void updateComponentsWidths(int[] colSizes) {
    for (Entry<Component, SmartGridLayoutConstraints> entry : components.entrySet()) {
      SmartGridLayoutConstraints c = entry.getValue();

      int width = 0;
      for (int idxCol = c.col; idxCol < c.colspan + c.col; idxCol++) {
        width += colSizes[idxCol];
      }

      Component comp = entry.getKey();
      if (log.isDebugEnabled()) {
        log.debug("Component: {}", comp);
        log.debug("\tCurrent size: {}", comp.getSize());
        log.debug("\tCurrent pref size: {}", comp.getPreferredSize());
      }

      Rectangle bounds = comp.getBounds();
      Rectangle newBounds = new Rectangle(bounds.x, bounds.y, width, comp.getHeight());
      if (!bounds.equals(newBounds)) {
        if (log.isDebugEnabled()) {
          log.debug("\tRequested width: {}", newBounds.width);
        }

        comp.setBounds(newBounds);

        if (comp.getBounds().getHeight() != bounds.getHeight()) {
          if (log.isDebugEnabled()) {
            log.debug("\tNew size: {}", comp.getSize());
            log.debug("\tNew pref size: {}", comp.getPreferredSize());
          }
        }
      }
    }
  }

  /**
   * Returns sum of non-weighted sizes
   *
   * @param askComponent what size to get when asking component
   */
  private void calcBaselineRowSizes(SizeResolver askComponent) {
    for (int idxRow = 0; idxRow < rows; idxRow++) {
      if (rowsSizesTypes[idxRow] == SIZE_TYPE_CONSTANT) {
        baselineRowSizes[idxRow] = rowsSizes[idxRow];
      } else if (rowsSizesTypes[idxRow] == SIZE_TYPE_ASKCOMPONENT) {
        int addedHeight = findMaxHeightWithinRow(idxRow, askComponent);
        baselineRowSizes[idxRow] = addedHeight;
      } else if (rowsSizesTypes[idxRow] == SIZE_TYPE_WEIGHTED) {
        int addedHeight = findMaxHeightWithinRow(idxRow, minimumHeight);
        baselineRowSizes[idxRow] = addedHeight;
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("calcBaselineRowSizes {}", Arrays.toString(baselineRowSizes));
    }
  }

  private int findMaxHeightWithinRow(int idxRow, SizeResolver sizeResolver) {
    int addedHeight = 0;
    for (int idxCol = 0; idxCol < cols; idxCol++) {
      Component comp = findComponentAt(idxCol, idxRow);
      if (comp == null) {
        continue;
      }
      SmartGridLayoutConstraints c = components.get(comp);
      if (c.rowspan > 1) {
        continue;
      }
      int compHeight = sizeResolver.get(comp);
      if (compHeight > addedHeight) {
        addedHeight = compHeight;
      }
    }
    if (addedHeight == 0) {
      log.warn(
          "findMaxHeightWithinRow(), rowIdx {} :: addedHeight is 0, which is not good", idxRow);
    }
    return addedHeight;
  }

  /**
   * Returns sum of non-weighted sizes
   *
   * @param askComponent what size to get when asking component
   */
  private void calcBaselineColSizes(SizeResolver askComponent) {
    for (int idxCol = 0; idxCol < cols; idxCol++) {
      if (colsSizesTypes[idxCol] == SIZE_TYPE_CONSTANT) {
        baselineColSizes[idxCol] = colsSizes[idxCol];
      } else if (colsSizesTypes[idxCol] == SIZE_TYPE_ASKCOMPONENT) {
        int addedWidth = findMaxWidthWithinColumn(idxCol, askComponent);
        baselineColSizes[idxCol] = addedWidth;
      } else if (colsSizesTypes[idxCol] == SIZE_TYPE_WEIGHTED) {
        int addedWidth = findMaxWidthWithinColumn(idxCol, minimumWidth);
        baselineColSizes[idxCol] = addedWidth;
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("calcBaselineColSizes {}", Arrays.toString(baselineColSizes));
    }
  }

  private int findMaxWidthWithinColumn(int idxCol, SizeResolver sizeResolver) {
    int addedWidth = 0;
    for (int idxRow = 0; idxRow < rows; idxRow++) {
      Component comp = findComponentAt(idxCol, idxRow);
      if (comp == null) {
        continue;
      }
      SmartGridLayoutConstraints c = components.get(comp);
      if (c.colspan > 1) {
        continue;
      }
      int compWidth = sizeResolver.get(comp);
      if (compWidth > addedWidth) {
        addedWidth = compWidth;
      }
    }
    if (addedWidth == 0) {
      log.warn(
          "findMaxWidthWithinColumn(), colIdx {} :: addedWidth is 0, which is not good", idxCol);
    }
    return addedWidth;
  }

  @Override
  public float getLayoutAlignmentX(Container target) {
    return 0.5f;
  }

  @Override
  public float getLayoutAlignmentY(Container target) {
    return 0.5f;
  }

  public interface SizeResolver {
    int get(Component c);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
