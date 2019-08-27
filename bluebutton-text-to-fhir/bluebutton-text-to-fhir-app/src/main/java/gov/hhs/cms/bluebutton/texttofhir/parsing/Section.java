package gov.hhs.cms.bluebutton.texttofhir.parsing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gov.hhs.cms.bluebutton.texttofhir.transform.EntryName;

/**
 * Models a section of a BlueButton text file, and the {@link Entry}s within it.
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
	 * Constructs a new {@link Section} instance.
	 * 
	 * @param index
	 *            the value to use for {@link #getIndex()}
	 * @param name
	 *            the value to use for {@link #getName()}
	 * @param entries
	 *            the value to use for {@link #getEntries()}
	 */
	public Section(long index, String name, Entry... entries) {
		this(index, name, Arrays.asList(entries));
	}

	/**
	 * @return the (zero-indexed) position of this section in
	 *         {@link TextFile#getSections()}
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
	 *         {@link Section} (which will often contain multiple {@link Entry}s
	 *         with the same {@link Entry#getName()})
	 */
	public List<Entry> getEntries() {
		return entries;
	}

	/**
	 * @param entryName
	 *            the {@link Entry#getName()} to match
	 * @return the first matching {@link Entry} found
	 */
	public Entry getEntry(String entryName) {
		return entries.stream().filter(e -> e.getName().equals(entryName)).findFirst().get();
	}

	/**
	 * @param entryName
	 *            the {@link Entry#getName()} to match
	 * @return the first matching {@link Entry} found
	 */
	public Entry getEntry(EntryName entryName) {
		return getEntry(entryName.getName());
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		output.append("Section[");
		output.append("name=");
		output.append(getName());
		output.append("]");
		return output.toString();
	}
}
