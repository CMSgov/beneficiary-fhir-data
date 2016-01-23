package gov.hhs.cms.bluebutton.texttofhir.parsing;

import java.util.Collections;
import java.util.List;

/**
 * Models a section of a BlueButton text file, and the {@link Entry}s
 * within it.
 */
public final class Section {
	private final long index;
	private final String name;
	private final List<Entry> entries;

	/**
	 * Constructs a new {@link Section} instance.
	 * 
	 * @param index
	 *            the value to use for {@link #getIndex()}
	 * @param name
	 *            the value to use for {@link #getName()}
	 * @param entries
	 *            the value to use for {@link #getEntries()}
	 */
	public Section(long index, String name, List<Entry> entries) {
		this.index = index;
		this.name = name;
		this.entries = Collections.unmodifiableList(entries);
	}

	/**
	 * @return the (zero-indexed) position of this section in {@link TextFile#getSections()}
	 */
	public long getIndex() {
		return index;
	}

	/**
	 * @return the name provided for this {@link Section} (which will be an
	 *         empty string for certain sections, e.g. claim subsections)
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the ordered {@link List} of {@link Entry}s in this
	 *         {@link Section} (which will often contain multiple
	 *         {@link Entry}s with the same {@link Entry#getName()})
	 */
	public List<Entry> getEntries() {
		return entries;
	}
}
