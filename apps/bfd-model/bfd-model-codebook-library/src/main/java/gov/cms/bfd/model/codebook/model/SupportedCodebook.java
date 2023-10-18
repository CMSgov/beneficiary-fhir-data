package gov.cms.bfd.model.codebook.model;

/** Enumerates the codebooks that are expected to be converted. */
public enum SupportedCodebook {
  /** The Beneficiary summary codebook which includes Medicare parts A/B/D. */
  BENEFICIARY_SUMMARY(
      "codebook-mbsf-abd.pdf",
      "Master Beneficiary Summary File - Base With Medicare Part A/B/D",
      "May 2017, Version 1.0"),

  /** The Beneficiary summary codebook which includes Medicare parts A/B/C/D. */
  BENEFICIARY_SUMMARY_PARTC(
      "codebook-mbsf-abcd.pdf",
      "Master Beneficiary Summary File - Base With Medicare Part A/B/C/D",
      "April 2019, Version 1.2"),

  /** The Medicare Fee-For-Service Claims codebook. */
  FFS_CLAIMS(
      "codebook-ffs-claims.pdf",
      "Medicare Fee-For-Service Claims (for Version K)",
      "December 2017, Version 1.4"),

  /** The Medicare Part D Event (PDE) / Drug Characteristics codebook. */
  PARTD_EVENTS(
      "codebook-pde.pdf",
      "Medicare Part D Event (PDE) / Drug Characteristics",
      "May 2017, Version 1.0");

  /** Describes the name of the resource file for the codebook. */
  private final String codebookPdfResourceName;

  /** The human-readable display name of the codebook. */
  private final String displayName;

  /** The version of the codebook. */
  private final String version;

  /**
   * Enum constant constructor.
   *
   * @param codebookPdfResourceName the name of the input/unparsed codebook PDF resource on this
   *     project's classpath
   * @param displayName the value to use for {@link #getDisplayName()}
   * @param version the value to use for {@link #getVersion()}
   */
  SupportedCodebook(String codebookPdfResourceName, String displayName, String version) {
    this.codebookPdfResourceName = codebookPdfResourceName;
    this.displayName = displayName;
    this.version = version;
  }

  /**
   * Gets the codebook xml resource name.
   *
   * @return the file/resource name of the XML resource
   */
  public String getCodebookXmlResourceName() {
    return codebookPdfResourceName.replace(".pdf", ".xml");
  }

  /**
   * Gets the codebook pdf resource name.
   *
   * @return the file/resource name of the PDF resource
   */
  public String getCodebookPdfResourceName() {
    return codebookPdfResourceName;
  }

  /**
   * Gets the codebook display name.
   *
   * @return the descriptive English name for this {@link SupportedCodebook}
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the codebook version.
   *
   * @return a human-readable {@link String} that identifies which version of the data is
   *     represented by this {@link SupportedCodebook}
   */
  public String getVersion() {
    return version;
  }
}
