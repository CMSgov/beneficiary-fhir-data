package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Claim;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests the methods of the {@link R4ClaimSamhsaMatcher}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class R4ClaimSamhsaMatcherTest {

  /** The class under test. */
  private R4ClaimSamhsaMatcher samhsaMatcher;

  /** The fiss claim transformer. */
  @Mock private FissClaimTransformerV2 mockFissTransformer;

  /** The mock mcs claim transformer. */
  @Mock private McsClaimTransformerV2 mockMcsTransformer;

  /** The mock claim returned by the transformers. */
  @Mock private Claim mockClaim;

  /** Sets up the class under test and dependencies. */
  @BeforeEach
  public void setup() {
    samhsaMatcher = new R4ClaimSamhsaMatcher(mockFissTransformer, mockMcsTransformer, false);
    when(mockFissTransformer.transform(any())).thenReturn(mockClaim);
    when(mockMcsTransformer.transform(any())).thenReturn(mockClaim);
    List<Claim.ProcedureComponent> procedureComponentList = new ArrayList<>();
    when(mockClaim.getProcedure()).thenReturn(procedureComponentList);
  }

  /**
   * Tests that the samhsa checker is invoked and returns true when a mcs claim with no data is
   * passed, as the matches should have nothing to match as a positive samhsa result.
   */
  @Test
  public void testHasNoSamhsaDataWhenMcsClaimResponseWithNoDataExpectTrue() {

    RdaMcsClaim mcsClaim = mock(RdaMcsClaim.class);
    Set<String> tags = new HashSet<>();
    ClaimWithSecurityTags<?> claim = new ClaimWithSecurityTags<>(mcsClaim, tags);
    boolean hasNoSamhsa = samhsaMatcher.hasNoSamhsaData(claim);

    assertTrue(hasNoSamhsa);
  }

  /**
   * Tests that the samhsa checker is invoked and returns true when a fiss claim with no data is
   * passed, as the matches should have nothing to match as a positive samhsa result.
   */
  @Test
  public void testHasNoSamhsaDataWhenFissClaimResponseWithNoDataExpectTrue() {

    RdaFissClaim fissClaim = mock(RdaFissClaim.class);
    Set<String> tags = new HashSet<>();
    ClaimWithSecurityTags<?> claim = new ClaimWithSecurityTags<>(fissClaim, tags);
    boolean hasNoSamhsa = samhsaMatcher.hasNoSamhsaData(claim);

    assertTrue(hasNoSamhsa);
  }
}
