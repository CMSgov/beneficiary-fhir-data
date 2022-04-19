package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import java.util.Optional;

public class TransformerContext {

  public MetricRegistry metricRegistry;
  public Object claim;
  public Optional<Boolean> includeTaxNumbers;
  public FdaDrugCodeDisplayLookup drugCodeDisplayLookup;

  public TransformerContext(
      MetricRegistry _metricRegistry,
      Object _claim,
      Optional<Boolean> _includeTaxNumbers,
      FdaDrugCodeDisplayLookup _drugCodeDisplayLookup) {
    this.metricRegistry = _metricRegistry;
    this.claim = _claim;
    this.includeTaxNumbers = _includeTaxNumbers;
    this.drugCodeDisplayLookup = _drugCodeDisplayLookup;
  }
}
