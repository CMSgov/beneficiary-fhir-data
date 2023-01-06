package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import java.util.Optional;

/**
 * Contains contextual information needed for transformation including configuration, metrics
 * registry and drug code display lookup.
 */
public class TransformerContext {

  /** The {@link MetricRegistry} for the overall application. */
  private final MetricRegistry metricRegistry;
  /**
   * The {@link Optional} populated with an {@link Boolean} to wheteher return tax numbers or not.
   */
  private final Optional<Boolean> includeTaxNumbers;
  /** The {@link FdaDrugCodeDisplayLookup} is to provide what drugCodeDisplay to return. */
  private final FdaDrugCodeDisplayLookup drugCodeDisplayLookup;

  /** The {@link NPIOrgLookup} is to provide what npi Org Name to Lookup to return. */
  private final NPIOrgLookup npiOrgLookup;

  /**
   * Instantiates a new {@link TransformerContext}.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param includeTaxNumbers the {@link Optional} populated with an {@link Boolean} to use
   * @param drugCodeDisplayLookup the {@link FdaDrugCodeDisplayLookup} to use
   * @param npiOrgLookup {@link NPIOrgLookup} to use
   */
  public TransformerContext(
      MetricRegistry metricRegistry,
      Optional<Boolean> includeTaxNumbers,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup,
      NPIOrgLookup npiOrgLookup) {
    this.metricRegistry = metricRegistry;
    this.includeTaxNumbers = includeTaxNumbers;
    this.drugCodeDisplayLookup = drugCodeDisplayLookup;
    this.npiOrgLookup = npiOrgLookup;
  }

  /**
   * Gets the {@link #metricRegistry}.
   *
   * @return the {@link MetricRegistry}
   */
  public MetricRegistry getMetricRegistry() {
    return metricRegistry;
  }

  /**
   * Gets the {@link #includeTaxNumbers}.
   *
   * @return the {@link Optional} populated with an {@link Boolean}
   */
  public Optional<Boolean> getIncludeTaxNumbers() {
    return includeTaxNumbers;
  }

  /**
   * Gets the {@link #drugCodeDisplayLookup}.
   *
   * @return the {@link FdaDrugCodeDisplayLookup}
   */
  public FdaDrugCodeDisplayLookup getDrugCodeDisplayLookup() {
    return drugCodeDisplayLookup;
  }

  /**
   * Gets the {@link #npiOrgLookup}.
   *
   * @return the {@link NPIOrgLookup}
   */
  public NPIOrgLookup getNPIOrgLookup() {
    return npiOrgLookup;
  }
}
