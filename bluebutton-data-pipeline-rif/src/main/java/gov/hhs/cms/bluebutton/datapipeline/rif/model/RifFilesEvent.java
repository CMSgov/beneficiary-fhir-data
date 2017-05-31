package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Models a RIF file event, in which a new set of RIF files have been made
 * available for processing.
 */
public final class RifFilesEvent {
	private final Instant timestamp;
	private final Set<RifFile> files;

	/**
	 * Constructs a new {@link RifFilesEvent} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param files
	 *            the value to use for {@link #getFiles()}
	 */
	public RifFilesEvent(Instant timestamp, Set<RifFile> files) {
		if (timestamp == null)
			throw new IllegalArgumentException();
		if (files == null)
			throw new IllegalArgumentException();
		if (files.isEmpty())
			throw new IllegalArgumentException();
		for (RifFile file : files)
			if (file == null)
				throw new IllegalArgumentException();

		this.timestamp = timestamp;
		this.files = files;
	}

	/**
	 * Constructs a new {@link RifFilesEvent} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param files
	 *            the value to use for {@link #getFiles()}
	 */
	public RifFilesEvent(Instant timestamp, RifFile... files) {
		this(timestamp, new HashSet<>(Arrays.asList(files)));
	}

	/**
	 * @return the timestamp that this event was fired at
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the {@link RifFile}s that are now available for processing
	 */
	public Set<RifFile> getFiles() {
		return files;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RifFilesEvent [timestamp=");
		builder.append(DateTimeFormatter.ISO_INSTANT.format(timestamp));
		builder.append(", files=");
		builder.append(files);
		builder.append("]");
		return builder.toString();
	}
}
