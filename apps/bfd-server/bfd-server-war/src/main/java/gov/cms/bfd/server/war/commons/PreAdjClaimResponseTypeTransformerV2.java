package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import org.hl7.fhir.r4.model.ClaimResponse;

/**
 * A {@link FunctionalInterface} describing the types that handle transforming FISS/MCS records into
 * FHIR {@link ClaimResponse} resources.
 */
@FunctionalInterface
public interface PreAdjClaimResponseTypeTransformerV2 {
  /**
   * Transforms the specified into a new FHIR {@link ClaimResponse} resource.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the FISS/MCS record to be transformed
   * @return a new FHIR {@link ClaimResponse} resource
   */
  ClaimResponse transform(MetricRegistry metricRegistry, Object claim);
}
