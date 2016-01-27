package gov.hhs.cms.bluebutton.texttofhir.parsing;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gov.hhs.cms.bluebutton.texttofhir.transform.SectionName;

/**
 * <p>
 * Models a CMS/MyMedicare.gov BlueButton text file in a very simple fashion: as
 * an ordered list of {@link Section}s, each of which is composed of an ordered
 * list of {@link Entry}s.
 * </p>
 * <p>
 * This is intentionally kept simple; more "high level" parsing is done in later
 * stages, against these model objects.
 * </p>
 */
public final class TextFile {
	private final ZonedDateTime timestamp;
	private final List<Section> sections;

	/**
	 * Constructs a new new {@link TextFile} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param sections
	 *            the value to use for {@link #getSections()}
	 */
	public TextFile(ZonedDateTime timestamp, List<Section> sections) {
		this.timestamp = timestamp;
		this.sections = Collections.unmodifiableList(sections);
	}

	/**
	 * Constructs a new new {@link TextFile} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param sections
	 *            the value to use for {@link #getSections()}
	 */
	public TextFile(ZonedDateTime timestamp, Section... sections) {
		this(timestamp, Arrays.asList(sections));
	}

	/**
	 * @return the timestamp included in the file, indicating when it was
	 *         generated
	 */
	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the ordered list of {@link Section}s in this {@link TextFile}
	 */
	public List<Section> getSections() {
		return sections;
	}

	/**
	 * @param sectionName
	 *            the {@link Section#getName()} to match
	 * @return the first {@link Section} found with the specified
	 *         {@link Section#getName()}
	 */
	public Section getSection(String sectionName) {
		return sections.stream().filter(s -> sectionName.equals(s.getName())).findFirst().get();
	}

	/**
	 * @param sectionName
	 *            the {@link Section#getName()} to match
	 * @return the first {@link Section} found with the specified
	 *         {@link Section#getName()}
	 */
	public Section getSection(SectionName sectionName) {
		return getSection(sectionName.getName());
	}
}
