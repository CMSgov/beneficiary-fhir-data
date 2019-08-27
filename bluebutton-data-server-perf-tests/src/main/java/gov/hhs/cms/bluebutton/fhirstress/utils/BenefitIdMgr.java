package gov.hhs.cms.bluebutton.fhirstress.utils;

/**
 * A utility method that helps manage an index into a RIF file containting
 *
 */
public class BenefitIdMgr implements BenefitIdManager {
	protected int currIndex;
	protected int minIndex;
	protected int maxIndex;
	protected String prefix;
	protected String format;

	/**
	 * Constructor(s)
	 *
	 * @param startIndex
	 *            a {@link int} containing the starting index to use for creating
	 *            benefit IDs
	 * @param minIndex
	 *            a {@link int} containing the minimum index allowed for creating
	 *            benefit IDs
	 * @param maxIndex
	 *            a {@link int} containing the maximum index allowed for creating
	 *            benefit IDs
	 * @param prefix
	 *            a {@link String} containing the prefix used for creating benefit
	 *            IDs
	 * @param format
	 *            a {@link String} containing the string formatter to use for
	 *            formatting indexes in benefit IDs
	 */
	public BenefitIdMgr(int startIndex, int minIndex, int maxIndex, String prefix, String format) {
		this.minIndex = minIndex;
		this.maxIndex = maxIndex;
		this.prefix = prefix;
		this.format = format;
		setCurrIndex(startIndex);
	}

	/**
	 * Private Methods
	 */

	/**
	 * Set the next index to the given value
	 * 
	 * @param index
	 *            a {@link int} value to set the next index to.
	 */
	private synchronized void setCurrIndex(int index) {
		if (index < minIndex) {
			index = minIndex;
		} else if (index > maxIndex) {
			index = maxIndex;
		}
		currIndex = index;
	}

	/**
	 * Public Methods
	 */
	
	/**
	 * Retrieves the next benefit id available by concatinating the prefix to the next
	 * index value and increments the current index
	 *
	 * @return a {@link String} containing the next benefit ID
	 */
	public synchronized String nextId() {
		String nextId = String.format(prefix + format, currIndex);
		incCurrIndex();
		return nextId;
	}

	/**
	 * Private Methods
	 */

	/**
	 * Increments the current index and applies boundary conditions to the value
	 */
	private synchronized void incCurrIndex() {
		if (++currIndex > maxIndex) {
			currIndex = minIndex;
		}
	}
};
