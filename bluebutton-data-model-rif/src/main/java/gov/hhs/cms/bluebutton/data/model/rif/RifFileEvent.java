package gov.hhs.cms.bluebutton.data.model.rif;

import java.util.Objects;

import com.codahale.metrics.MetricRegistry;

/**
 * Models a single {@link RifFile} within a {@link RifFilesEvent}.
 */
public final class RifFileEvent {
	private final MetricRegistry eventMetrics;
	private final RifFilesEvent parentFilesEvent;
	private final RifFile file;

	/**
	 * Constructs a new {@link RifFileEvent} instance.
	 * 
	 * @param parentFilesEvent
	 *            the value to use for {@link #getParentFilesEvent()}
	 * @param file
	 *            the value to use for {@link #getFile()}
	 */
	RifFileEvent(RifFilesEvent parentFilesEvent, RifFile file) {
		Objects.requireNonNull(parentFilesEvent);
		Objects.requireNonNull(file);

		this.eventMetrics = new MetricRegistry();

		this.parentFilesEvent = parentFilesEvent;
		this.file = file;
	}

	/**
	 * @return the {@link MetricRegistry} that should be used to record the work
	 *         done to process this {@link RifFileRecords}
	 */
	public MetricRegistry getEventMetrics() {
		return eventMetrics;
	}

	/**
	 * @return the {@link RifFilesEvent} that this {@link RifFileEvent} is a
	 *         part of
	 */
	public RifFilesEvent getParentFilesEvent() {
		return parentFilesEvent;
	}

	/**
	 * @return the {@link RifFile} represented by this {@link RifFileEvent}
	 */
	public RifFile getFile() {
		return file;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RifFileEvent [parentFilesEvent.timestamp=");
		builder.append(parentFilesEvent.getTimestamp());
		builder.append(", file=");
		builder.append(file);
		builder.append("]");
		return builder.toString();
	}
}
