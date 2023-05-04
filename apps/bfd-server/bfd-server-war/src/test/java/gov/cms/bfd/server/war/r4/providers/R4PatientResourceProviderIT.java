package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import java.util.List;
import javax.persistence.EntityManager;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Integration tests for {@link R4PatientResourceProvider}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class R4PatientResourceProviderIT {

  /** The Patient resource provider. */
  private R4PatientResourceProvider patientResourceProvider;

  /** The mocked request details. */
  @Mock private ServletRequestDetails requestDetails;

  /** The mocked input id value. */
  @Mock IdType patientId;

  /** The Metric registry. */
  @Mock private MetricRegistry metricRegistry;
  /** The Loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;
  /** The mock entity manager for mocking database calls. */
  @Mock private EntityManager entityManager;
  /** The Beneficiary transformer. */
  private BeneficiaryTransformerV2 beneficiaryTransformer;

  /** The Test data bene. */
  private Beneficiary testDataBene;

  /**
   * A list of expected historical mbis for adding to the sample A loaded data (as data coming back
   * from the endpoint will have this added in the resource provider).
   */
  private static final List<String> standardExpectedHistoricalMbis =
      List.of("9AB2WW3GR44", "3456689");

  /** Sets the test resources up. */
  @BeforeEach
  public void setup() {
    beneficiaryTransformer = new BeneficiaryTransformerV2(metricRegistry);

    // Steal mock setup from R4PatientResourceProviderTest
  }

  // TODO: Add ITs if/as needed to test boundary between transformer and resource provider
}
