package gov.hhs.cms.bluebutton.data.model.rif;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Models a RIF file event, in which a new set of RIF files have been made
 * available for processing.
 */
public final class RifFilesEvent {
	private final Instant timestamp;
	private final List<RifFileEvent> fileEvents;

	/**
	 * Constructs a new {@link RifFilesEvent} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param files
	 *            the value to use for {@link #getFileEvents()}
	 */
	public RifFilesEvent(Instant timestamp, List<RifFile> files) {
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

		this.fileEvents = buildFileEvents(files);
	}

	/**
	 * Produces the properly-sorted-for-safe-processing {@link List} of
	 * {@link RifFileEvent}s for this {@link RifFilesEvent}. The ordering
	 * constraint is that {@link RifFileType#BENEFICIARY} files must always be
	 * processed first (if more than one {@link RifFile} is available). This is
	 * necessary in order to avoid foreign key violations when processing the
	 * claims that rely on them.
	 * 
	 * @param files
	 *            the {@link RifFile}s to produce {@link RifFileEvent}s for
	 * @return the {@link RifFileEvent}s for the specified {@link RifFile}s
	 */
	private List<RifFileEvent> buildFileEvents(List<RifFile> files) {
		List<RifFileEvent> fileEvents = new ArrayList<>(files.size());
		for (RifFile file : files)
			fileEvents.add(new RifFileEvent(this, file));

		Comparator<RifFileEvent> fileEventsSorter = new Comparator<RifFileEvent>() {
			@Override
			public int compare(RifFileEvent o1, RifFileEvent o2) {
				if (o1.getFile().getFileType() == RifFileType.BENEFICIARY
						&& o2.getFile().getFileType() != RifFileType.BENEFICIARY)
					return -1;
				else if (o1.getFile().getFileType() != RifFileType.BENEFICIARY
						&& o2.getFile().getFileType() == RifFileType.BENEFICIARY)
					return 1;
				else
					return Integer.compare(o1.getFile().getFileType().ordinal(), o2.getFile().getFileType().ordinal());
			}
		};
		Collections.sort(fileEvents, fileEventsSorter);

		return fileEvents;
	}

	/**
	 * Constructs a new {@link RifFilesEvent} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param files
	 *            the value to use for {@link #getFileEvents()}
	 */
	public RifFilesEvent(Instant timestamp, RifFile... files) {
		this(timestamp, Arrays.asList(files));
	}

	/**
	 * @return the timestamp that this event was fired at
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the {@link RifFileEvent}s contained in this {@link RifFilesEvent}
	 */
	public List<RifFileEvent> getFileEvents() {
		return fileEvents;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RifFilesEvent [timestamp=");
		builder.append(DateTimeFormatter.ISO_INSTANT.format(timestamp));
		builder.append(", fileEvents=");
		builder.append(fileEvents);
		builder.append("]");
		return builder.toString();
	}
}
