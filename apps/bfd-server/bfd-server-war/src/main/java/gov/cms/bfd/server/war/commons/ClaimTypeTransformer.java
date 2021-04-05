package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

/**
 * A {@link FunctionalInterface} describing the types that handle transforming CCW RIF record (e.g.
 * {@link CarrierClaim}, {@link DMEClaim}, etc.) into FHIR {@link ExplanationOfBenefit} resources.
 */
@FunctionalInterface
public interface ClaimTypeTransformer {
  /**
   * Transforms the specified (e.g. {@link CarrierClaim}, {@link DMEClaim}, etc.) into a new FHIR
   * {@link ExplanationOfBenefit} resource.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param rifRecord the CCW RIF record (e.g. {@link CarrierClaim}, {@link DMEClaim}, etc.) to be
   *     transformed
   * @param includeTaxNumbers whether or not to include tax numbers in the result (see {@link
   *     #HEADER_NAME_INCLUDE_TAX_NUMBERS}, defaults to <code>false</code>)
   * @return a new FHIR {@link ExplanationOfBenefit} resource
   */
  ExplanationOfBenefit transform(
      MetricRegistry metricRegistry, Object rifRecord, Optional<Boolean> includeTaxNumbers);
}
