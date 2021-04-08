package gov.cms.bfd.server.war.commons.adapter;

import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.r4.providers.ClaimTypeV2;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OutpatientClaimAdaptor extends ClaimAdaptor {
  private OutpatientClaim claim;

  @Override
  public boolean is(ClaimTypeV2 compare) {
    return ClaimTypeV2.OUTPATIENT.equals(compare);
  }

  public OutpatientClaimAdaptor(OutpatientClaim claim) {
    this.claim = claim;
  }

  @Override
  public Optional<String> getClaimId() {
    return Optional.of(claim.getClaimId());
  }

  @Override
  public Optional<String> getBeneficiaryId() {
    return Optional.of(claim.getBeneficiaryId());
  }

  @Override
  public Optional<ClaimTypeV2> getClaimType() {
    return Optional.of(ClaimTypeV2.OUTPATIENT);
  }

  @Override
  public Optional<BigDecimal> getClaimGroupId() {
    return Optional.of(claim.getClaimGroupId());
  }

  @Override
  public Optional<MedicareSegment> getMedicareSegment() {
    return Optional.of(MedicareSegment.PART_B);
  }

  @Override
  public Optional<LocalDate> getDateFrom() {
    return Optional.of(claim.getDateFrom());
  }

  @Override
  public Optional<LocalDate> getDateThrough() {
    return Optional.of(claim.getDateThrough());
  }

  @Override
  public Optional<BigDecimal> getPaymentAmount() {
    return Optional.of(claim.getPaymentAmount());
  }

  @Override
  public Optional<Character> getFinalAction() {
    return Optional.of(claim.getFinalAction());
  }

  @Override
  public Optional<String> getProfile() {
    return Optional.of(ProfileConstants.C4BB_EOB_OUTPATIENT_PROFILE_URL);
  }

  @Override
  public Optional<Character> getNearLineRecordIdCode() {
    return Optional.of(claim.getNearLineRecordIdCode());
  }

  @Override
  public Optional<String> getClaimTypeCode() {
    return Optional.of(claim.getClaimTypeCode());
  }

  // NationalDrugCode not here

  @Override
  public List<ClaimLineAdaptorInterface> getLines() {
    return claim.getLines().stream()
        .map(line -> new OutpatientClaimLineAdaptor(line))
        .collect(Collectors.toList());
  }
}
