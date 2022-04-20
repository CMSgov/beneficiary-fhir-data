package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import java.util.Optional;

/**
 * Contains contextual information needed for transformation including configuration, metrics
 * registry and drug code display lookup.
 */
public class TransformerContext {

  private final MetricRegistry metricRegistry;
  private final Optional<Boolean> includeTaxNumbers;
  private final FdaDrugCodeDisplayLookup drugCodeDisplayLookup;

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param includeTaxNumbers the {@link Optional} populated with an {@link Boolean} to use
   * @param drugCodeDisplayLookup the {@link FdaDrugCodeDisplayLookup} to use
   */
  public TransformerContext(
      MetricRegistry metricRegistry,
      Optional<Boolean> includeTaxNumbers,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup) {
    this.metricRegistry = metricRegistry;
    this.includeTaxNumbers = includeTaxNumbers;
    this.drugCodeDisplayLookup = drugCodeDisplayLookup;
  }

  /** @return the {@link MetricRegistry} */
  public MetricRegistry getMetricRegistry() {
    return metricRegistry;
  }

  /** @return the {@link Optional} populated with an {@link Boolean} */
  public Optional<Boolean> getIncludeTaxNumbers() {
    return includeTaxNumbers;
  }

  /** @return the {@link FdaDrugCodeDisplayLookup} */
  public FdaDrugCodeDisplayLookup getDrugCodeDisplayLookup() {
    return drugCodeDisplayLookup;
  }
}
