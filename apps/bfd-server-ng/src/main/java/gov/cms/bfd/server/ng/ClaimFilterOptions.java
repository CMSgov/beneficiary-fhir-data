package gov.cms.bfd.server.ng;

import lombok.Getter;

@Getter
public final class ClaimFilterOptions {
  private final SamhsaFilterMode samhsaFilterMode;
  private final boolean includeTaxNumber;
  private final ClaimSecurityStatus securityStatus;

  private ClaimFilterOptions(Builder builder) {
    this.samhsaFilterMode = builder.samhsaFilterMode;
    this.includeTaxNumber = builder.includeTaxNumber;
    this.securityStatus = builder.securityStatus;
  }

  public ClaimFilterOptions withSecurityStatus(ClaimSecurityStatus securityStatus) {
    return builder()
        .samhsaFilterMode(this.samhsaFilterMode)
        .includeTaxNumber(this.includeTaxNumber)
        .securityStatus(securityStatus)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    // Sensible defaults (hopefully)
    private SamhsaFilterMode samhsaFilterMode = SamhsaFilterMode.EXCLUDE;
    private boolean includeTaxNumber = false;
    private ClaimSecurityStatus securityStatus = ClaimSecurityStatus.NONE;

    public Builder samhsaFilterMode(SamhsaFilterMode samhsaFilterMode) {
      this.samhsaFilterMode = samhsaFilterMode;
      return this;
    }

    public Builder includeTaxNumber(boolean includeTaxNumber) {
      this.includeTaxNumber = includeTaxNumber;
      return this;
    }

    public Builder securityStatus(ClaimSecurityStatus securityStatus) {
      this.securityStatus = securityStatus;
      return this;
    }

    public ClaimFilterOptions build() {
      return new ClaimFilterOptions(this);
    }
  }
}
