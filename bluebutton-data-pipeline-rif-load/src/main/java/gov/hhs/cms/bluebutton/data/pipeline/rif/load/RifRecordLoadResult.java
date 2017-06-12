package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;

/**
 * Represents the results of successful {@link RifLoader} operations on
 * individual {@link RifRecordEvent}s.
 */
public final class RifRecordLoadResult {
	private final RifRecordEvent<?> rifRecordEvent;
	private final LoadAction loadAction;

	/**
	 * Constructs a new {@link RifRecordLoadResult}.
	 * 
	 * @param rifRecordEvent
	 *            the value to use for {@link #getRifRecordEvent()}
	 * @param loadAction
	 *            the value to use for {@link #getLoadAction()}
	 */
	public RifRecordLoadResult(RifRecordEvent<?> rifRecordEvent, LoadAction loadAction) {
		if (rifRecordEvent == null)
			throw new IllegalArgumentException();
		if (loadAction == null)
			throw new IllegalArgumentException();

		this.rifRecordEvent = rifRecordEvent;
		this.loadAction = loadAction;
	}

	/**
	 * @return the {@link RifRecordEvent} that was loaded
	 */
	public RifRecordEvent<?> getRifRecordEvent() {
		return rifRecordEvent;
	}

	/**
	 * @return the {@link LoadAction} that indicates the outcome of the load
	 */
	public LoadAction getLoadAction() {
		return loadAction;
	}

	/**
	 * Enumerates the types of actions that a load operation may have resulted
	 * in on the database.
	 */
	public static enum LoadAction {
		/**
		 * Indicates that the record(s) were successfully loaded to the
		 * database.
		 */
		LOADED;

		// TODO ALREADY_PRESENT
	}
}
