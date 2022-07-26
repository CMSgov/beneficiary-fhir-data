package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.data.fda.lookup.fdadrugcodelookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
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
   * @param transformerContext the {@link TransformerContext} to use
   * @param claim the {@link Object} to use
   * @return a new FHIR {@link FdaDrugCodeDisplayLookup} resource
   */
  ExplanationOfBenefit transform(TransformerContext transformerContext, Object claim);
}
