package gov.hhs.cms.bluebutton.fhirstress.utils;

public class BenefitIdMgr {
  protected int currIndex;
  protected int minIndex;
  protected int maxIndex;
  protected String prefix;
  protected String format;

  /** 
   * Constructor(s) 
   *
   * @param startIndex
   *  a {@link int} containing the starting index to use for creating benefit IDs
   * @param minIndex
   *  a {@link int} containing the minimum index allowed for creating benefit IDs
   * @param maxIndex
   *  a {@link int} containing the maximum index allowed for creating benefit IDs
   * @param prefix
   *  a {@link String} containing the prefix used for creating benefit IDs
   * @param format
   *  a {@link String} containing the string formatter to use for formatting
   *  indexes in benefit IDs
   */
  public BenefitIdMgr(int startIndex, int minIndex, int maxIndex, String prefix, String format) {
    this.minIndex = minIndex;
    this.maxIndex = maxIndex;
    this.prefix = prefix;
    this.format = format;
    setCurrIndex(startIndex);
  }

  /** 
   * Public Methods 
   */

  /**
   * Set the next index to the given value
   * 
   * @param index
   *  a {@link int} value to set the next index to. 
   */
  public void setCurrIndex(int index) {
    if (index < minIndex) {
      index = minIndex;
    }
    else if(index > maxIndex) {
      index = maxIndex;
    }
    currIndex = index;
  }

  /**
   * Retrieves the next benefit id available by concating the prefix to the
   * next index value and increments the current index
   *
   * @return
   *  a {@link String} containing the next benefit ID 
   */
  public String nextId() {
    String nextId = String.format(prefix + format, currIndex);
    incCurrIndex();
    return nextId;
  }

  /**
   * Retrieves the previous benefit id available by concating the prefix to the
   * previous index value and decrements the current index
   *
   * @return
   *  a {@link String} containing the previous benefit ID 
   */
  public String prevId() {
    decCurrIndex();
    return String.format(prefix + format, currIndex);
  }

  /** 
   * Private Methods 
   */

  /**
   * Increments the current index and applies boundary conditions to the value
   */
  private void incCurrIndex() {
    if (++currIndex > maxIndex) {
      currIndex = minIndex;
    }
  }

  /**
   * Decrements the current index and applies boundary conditions to the value
   */
  private void decCurrIndex() {
    if (--currIndex < minIndex) {
      currIndex = maxIndex;
    }
  }

};
