package ru.skarpushin.swingpm.modelprops.virtualtable;

/** Table might call this to notify if it didn't find data for row */
public interface RowRetrieverFeedbackHandler {
  /**
   * NOTE for Impl: It must be blazing fast since this method called while table requesting for new
   * data to be rendered
   */
  void handleRowRequested(int rowIndex, boolean isDataFound);
}
