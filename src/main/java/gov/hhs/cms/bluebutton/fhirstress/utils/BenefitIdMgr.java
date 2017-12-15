package gov.hhs.cms.bluebutton.fhirstress.utils;

public class BenefitIdMgr {
  protected int currIndex;
  protected int minIndex;
  protected int maxIndex;
  protected String prefix;
  protected String format;

  /** 
   * Constructor(s) 
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
   */
  public String nextId() {
    String nextId = String.format(prefix + format, currIndex);
    incCurrIndex();
    return nextId;
  }

  /**
   * Retrieves the previous benefit id available by concating the prefix to the
   * previous index value and decrements the current index
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
