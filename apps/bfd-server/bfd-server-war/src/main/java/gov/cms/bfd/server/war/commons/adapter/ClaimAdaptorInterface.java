package gov.cms.bfd.server.war.commons.adapter;

import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.r4.providers.ClaimTypeV2;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClaimAdaptorInterface {
  public boolean is(ClaimTypeV2 compare);

  public Optional<String> getClaimId();

  public Optional<String> getBeneficiaryId();

  public Optional<ClaimTypeV2> getClaimType();

  public Optional<BigDecimal> getClaimGroupId();

  public Optional<MedicareSegment> getMedicareSegment();

  public Optional<LocalDate> getDateFrom();

  public Optional<LocalDate> getDateThrough();

  public Optional<BigDecimal> getPaymentAmount();

  public Optional<Character> getFinalAction();

  public Optional<String> getProfile();

  public Optional<Character> getNearLineRecordIdCode();

  public Optional<String> getClaimTypeCode();

  public List<ClaimLineAdaptorInterface> getLines();
}
