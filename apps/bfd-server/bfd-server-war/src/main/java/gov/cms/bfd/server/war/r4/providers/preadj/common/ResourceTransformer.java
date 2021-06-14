package gov.cms.bfd.server.war.r4.providers.preadj.common;

import com.codahale.metrics.MetricRegistry;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ClaimResponse;

/**
 * A {@link FunctionalInterface} describing the types that handle transforming FISS/MCS records into
 * FHIR resources.
 *
 * @param <T> The FHIR resource type to transform to.
 */
@FunctionalInterface
public interface ResourceTransformer<T extends IBaseResource> {
  /**
   * Transforms the specified into a new FHIR {@link ClaimResponse} resource.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the FISS/MCS record to be transformed
   * @return a new FHIR resource
   */
  T transform(MetricRegistry metricRegistry, Object claim);
}
