package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import org.hl7.fhir.r4.model.Claim;

/**
 * A {@link FunctionalInterface} describing the types that handle transforming FISS/MCS records into
 * FHIR {@link Claim} resources.
 */
@FunctionalInterface
public interface PreAdjClaimTypeTransformerV2 {
  /**
   * Transforms the specified into a new FHIR {@link Claim} resource.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the FISS/MCS record to be transformed
   * @return a new FHIR {@link Claim} resource
   */
  Claim transform(MetricRegistry metricRegistry, Object claim);
}
