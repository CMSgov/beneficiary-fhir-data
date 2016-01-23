package gov.hhs.cms.bluebutton.texttofhir.parsing;

/**
 * Models a key and value pair in a BlueButton text file.
 */
public final class Entry {
	private final long index;
	private final String name;
	private final String value;

	/**
	 * Constructs a new {@link Entry} instance.
	 * 
	 * @param index
	 *            the value to use for {@link #getIndex()}
	 * @param name
	 *            the value to use for {@link #getName()}
	 * @param value
	 *            the value to use for {@link #getValue()}
	 */
	public Entry(long index, String name, String value) {
		this.index = index;
		this.name = name;
		this.value = value;
	}

	/**
	 * @return the (zero-indexed) position of this entry in its parent
	 *         {@link Section#getEntries()}
	 */
	public long getIndex() {
		return index;
	}

	/**
	 * @return the name of this {@link Entry} (which will often not be
	 *         unique within the {@link Section})
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the value of this {@link Entry}
	 */
	public String getValue() {
		return value;
	}
}
