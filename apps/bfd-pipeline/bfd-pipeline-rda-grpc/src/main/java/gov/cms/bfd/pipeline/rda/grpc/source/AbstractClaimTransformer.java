package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.mpsm.rda.v1.RecordSource;
import java.util.Map;

/** Base class for Claim transformation offering common logic shared between FISS and MCS */
public abstract class AbstractClaimTransformer {

  /** Maps strings sent by RDA to simple numeric values for DB storage */
  private static final Map<String, Short> PHASE_TO_SHORT =
      Map.of(
          "P1", (short) 1,
          "P2", (short) 2,
          "P3", (short) 3);

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

    transformer.copyOptionalUIntToShort(
        RdaClaimMessageMetaData.Fields.phase,
        () -> {
          boolean hasPhase = from.hasPhase();

          if (hasPhase && !PHASE_TO_SHORT.containsKey(from.getPhase())) {
            transformer.addError(RdaChange.Source.Fields.phase, "is unknown phase");
            hasPhase = false;
          }

          return hasPhase;
        },
        () -> PHASE_TO_SHORT.get(from.getPhase()),
        to::setPhase);
    transformer.copyOptionalUIntToShort(
        RdaChange.Source.Fields.phaseSeqNum,
        from::hasPhaseSeqNum,
        from::getPhaseSeqNum,
        to::setPhaseSeqNum);
    transformer.copyOptionalDate(
        RdaChange.Source.Fields.extractDate,
        from::hasExtractDate,
        from::getExtractDate,
        to::setExtractDate);
    transformer.copyOptionalTimestamp(
        RdaChange.Source.Fields.transmissionTimestamp,
        from::hasTransmissionTimestamp,
        from::getTransmissionTimestamp,
        to::setTransmissionTimestamp);

    return to;
  }
}
