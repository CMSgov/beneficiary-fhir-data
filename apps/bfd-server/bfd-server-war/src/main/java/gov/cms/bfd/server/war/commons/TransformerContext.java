package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import java.util.Optional;

/**
 * Contains all of the boolean logic used to transform include tax numbers and which drug code
 * display to use based on the environment
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

  /** @return the short identifier for this {@link MetricRegistry} */
  public MetricRegistry getMetricRegistry() {
    return metricRegistry;
  }

  /** @return the short identifier for this {@link Optional} populated with an {@link Boolean} */
  public Optional<Boolean> getIncludeTaxNumbers() {
    return includeTaxNumbers;
  }

  /** @return the short identifier for this {@link FdaDrugCodeDisplayLookup} */
  public FdaDrugCodeDisplayLookup getDrugCodeDisplayLookup() {
    return drugCodeDisplayLookup;
  }
}
