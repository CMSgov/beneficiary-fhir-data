package gov.cms.bfd.server.war.commons.adapter;

import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.r4.providers.ClaimTypeV2;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class ClaimAdaptor implements ClaimAdaptorInterface {
  public abstract boolean is(ClaimTypeV2 compare);

  public Optional<String> getClaimId() {
    return Optional.empty();
  }

  public Optional<String> getBeneficiaryId() {
    return Optional.empty();
  }

  public Optional<ClaimTypeV2> getClaimType() {
    return Optional.empty();
  }

  public Optional<BigDecimal> getClaimGroupId() {
    return Optional.empty();
  }

  public Optional<MedicareSegment> getMedicareSegment() {
    return Optional.empty();
  }

  public Optional<LocalDate> getDateFrom() {
    return Optional.empty();
  }

  public Optional<LocalDate> getDateThrough() {
    return Optional.empty();
  }

  public Optional<BigDecimal> getPaymentAmount() {
    return Optional.empty();
  }

  public Optional<Character> getFinalAction() {
    return Optional.empty();
  }

  public Optional<String> getProfile() {
    return Optional.empty();
  }

  public Optional<Character> getNearLineRecordIdCode() {
    return Optional.empty();
  }

  public Optional<String> getClaimTypeCode() {
    return Optional.empty();
  }

  public List<ClaimLineAdaptorInterface> getLines() {
    return Collections.emptyList();
  }
}
