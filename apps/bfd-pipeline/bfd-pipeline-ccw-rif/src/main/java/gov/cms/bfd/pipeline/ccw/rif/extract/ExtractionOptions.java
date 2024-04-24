package gov.cms.bfd.pipeline.ccw.rif.extract;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientConfig;
import jakarta.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.Getter;

/** Models the user-configurable options for extraction of RIF data from S3. */
public final class ExtractionOptions {

  /** The S3 bucket name. */
  @Getter private final String s3BucketName;

  /** The allowed rif file type for this extraction. */
  @Nullable private final RifFileType allowedRifFileType;

  /** The max keys for S3. */
  @Nullable private final Integer s3ListMaxKeys;

  /** Common config settings used to configure S3 clients. */
  @Getter private final S3ClientConfig s3ClientConfig;

  /**
   * Initializes an instance.
   *
   * @param s3BucketName the value to use for {@link #s3BucketName}
   * @param allowedRifFileType the value to use for {@link #getDataSetFilter()}
   * @param s3ListMaxKeys the value to use for {@link #getS3ListMaxKeys()}
   * @param s3ClientConfig used to configure S3 clients
   */
  public ExtractionOptions(
      String s3BucketName,
      Optional<RifFileType> allowedRifFileType,
      Optional<Integer> s3ListMaxKeys,
      S3ClientConfig s3ClientConfig) {
    this.s3BucketName = s3BucketName;
    this.allowedRifFileType = allowedRifFileType.orElse(null);
    this.s3ListMaxKeys = s3ListMaxKeys.orElse(null);
    this.s3ClientConfig = s3ClientConfig;
  }

  /**
   * Initializes an instance with a {@link #getDataSetFilter()} that doesn't skip anything and using
   * default AWS region for S3.
   *
   * @param s3BucketName the value to use for {@link #s3BucketName}
   */
  public ExtractionOptions(String s3BucketName) {
    this(s3BucketName, Optional.empty(), Optional.empty(), S3ClientConfig.s3Builder().build());
  }

  /**
   * Gets the allowed rif file type.
   *
   * @return the single {@link RifFileType} that the application should process, or {@link
   *     Optional#empty()} if it should process all {@link RifFileType}s (when set, any data sets
   *     that do not <strong>only</strong> contain the specified {@link RifFileType} will be skipped
   *     by the application)
   */
  public Optional<RifFileType> getAllowedRifFileType() {
    return Optional.ofNullable(allowedRifFileType);
  }

  /**
   * Gets the data set filter.
   *
   * @return a {@link Predicate} that returns {@code true} for {@link Predicate#test(Object)} if the
   *     specified {@link DataSetManifest} matches the {@link #getAllowedRifFileType()} value, and
   *     {@code false} if it does not (and thus should be skipped)
   */
  public Predicate<DataSetManifest> getDataSetFilter() {
    if (allowedRifFileType != null)
      return d ->
          d.getEntries().stream().map(e -> e.getType()).allMatch(t -> allowedRifFileType == t);
    else return e -> true;
  }

  /**
   * Note: This method is intended for test purposes: setting this value to {@code 1} in tests can
   * help to verify the S3 paging logic.
   *
   * @return the value to use for page size in all S3 list operations
   */
  public Optional<Integer> getS3ListMaxKeys() {
    return Optional.ofNullable(s3ListMaxKeys);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ExtractionOptions [s3BucketName=");
    builder.append(s3BucketName);
    builder.append(", allowedRifFileType=");
    builder.append(allowedRifFileType);
    builder.append("]");
    return builder.toString();
  }
}
