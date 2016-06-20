package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import org.hl7.fhir.dstu21.model.Bundle;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;

/**
 * Models a completed {@link DataTransformer#transform(rx.Observable)}
 * operation, for a single element.
 */
public final class TransformedBundle {
	private final RifRecordEvent<?> source;
	private final Bundle result;

	/**
	 * Constructs a new {@link TransformedBundle} instance.
	 * 
	 * @param source
	 *            the value to use for {@link #getSource()}
	 * @param result
	 *            the value to use for {@link #getResult()}
	 */
	public TransformedBundle(RifRecordEvent<?> source, Bundle result) {
		this.source = source;
		this.result = result;
	}

	/**
	 * @return the source {@link RifRecordEvent} that was transformed
	 */
	public RifRecordEvent<?> getSource() {
		return source;
	}

	/**
	 * @return the FHIR {@link Bundle} that was produced by the transformation
	 */
	public Bundle getResult() {
		return result;
	}
}
