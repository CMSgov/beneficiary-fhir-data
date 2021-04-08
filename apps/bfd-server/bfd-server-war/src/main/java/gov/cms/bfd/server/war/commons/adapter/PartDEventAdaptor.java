package gov.cms.bfd.server.war.commons.adapter;

import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.r4.providers.ClaimTypeV2;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PartDEventAdaptor extends ClaimAdaptor {
  private PartDEvent claim;

  @Override
  public boolean is(ClaimTypeV2 compare) {
    return ClaimTypeV2.PDE.equals(compare);
  }

  public PartDEventAdaptor(PartDEvent claim) {
    this.claim = claim;
  }

  @Override
  public Optional<String> getClaimId() {
    return Optional.of(claim.getEventId());
  }

  @Override
  public Optional<String> getBeneficiaryId() {
    return Optional.of(claim.getBeneficiaryId());
  }

  @Override
  public Optional<ClaimTypeV2> getClaimType() {
    return Optional.of(ClaimTypeV2.PDE);
  }

  @Override
  public Optional<BigDecimal> getClaimGroupId() {
    return Optional.of(claim.getClaimGroupId());
  }

  @Override
  public Optional<MedicareSegment> getMedicareSegment() {
    return Optional.of(MedicareSegment.PART_D);
  }

  /* DateFrom, Through, PaymentAmount all default impl (empty) */

  @Override
  public Optional<Character> getFinalAction() {
    return Optional.of(claim.getFinalAction());
  }

  @Override
  public Optional<String> getProfile() {
    return Optional.of(ProfileConstants.C4BB_EOB_PHARMACY_PROFILE_URL);
  }

  /* NearLineRecordIdCode and ClaimTypeCode also not in PDE */

  @Override
  public List<ClaimLineAdaptorInterface> getLines() {
    return Arrays.asList(new PartDEventClaimLineAdaptor(claim));
  }
}
