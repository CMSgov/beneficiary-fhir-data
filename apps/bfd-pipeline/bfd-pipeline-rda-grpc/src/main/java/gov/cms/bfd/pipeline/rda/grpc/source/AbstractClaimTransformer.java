package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.mpsm.rda.v1.RecordSource;

/** Base class for Claim transformation offering common logic shared between FISS and MCS */
public abstract class AbstractClaimTransformer {

  /**
   * Validates and copies the {@link RecordSource} phase, phaseSequence, and transmissionTimestamp
   * to an {@link RdaChange.Source} object to be returned.
   *
   * @param from The {@link RecordSource} to copy data from
   * @param transformer The {@link DataTransformer} to use to validate and copy the data
   * @return The generated {@link RdaChange.Source} object.
   */
  protected RdaChange.Source transformSource(RecordSource from, DataTransformer transformer) {
    RdaChange.Source to = new RdaChange.Source();

    transformer.copyOptionalString(
        RdaClaimMessageMetaData.Fields.phase, 2, 2, from::hasPhase, from::getPhase, to::setPhase);
    transformer.copyOptionalUIntToShort(
        RdaClaimMessageMetaData.Fields.phaseSeqNumber,
        from::hasPhaseSeqNum,
        from::getPhaseSeqNum,
        to::setPhaseSeqNum);
    transformer.copyOptionalString(
        RdaClaimMessageMetaData.Fields.transmissionTimestamp,
        10,
        30,
        from::hasTransmissionTimestamp,
        from::getTransmissionTimestamp,
        to::setTransmissionTimestamp);

    return to;
  }
}
