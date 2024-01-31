package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.spark.util.sketch.BloomFilter;

/**
 * LoadedFile filters are used to determine if a given beneficiary was updated in particular loaded
 * file. Beneath the covers, they use BloomFilters (see <a
 * href="https://en.wikipedia.org/wiki/Bloom_filter">Bloom Filters</a>) which are space efficient.
 */
@Getter
@AllArgsConstructor
public class LoadedFileFilter {
  /** False positive percentage value used in creating the bloom filter. */
  public static final double FALSE_POSITIVE_PERCENTAGE = 0.01;

  /** The entry of the LoadedFiles table. */
  private final long loadedFileId;

  /** The count of batches in the filters. */
  private final int batchesCount;

  /** The start time the RIF load took place. */
  private final Instant firstUpdated;

  /** The end time the RIF load took place. */
  private final Instant lastUpdated;

  /** The beneficiaries that were updated in the RIF load. */
  private final BloomFilter updatedBeneficiaries;

  /**
   * Tests the filter's time span overlaps the passed in date range.
   *
   * @param dateRangeParam to compare
   * @return true if there is some overlap
   */
  public boolean matchesDateRange(DateRangeParam dateRangeParam) {
    if (dateRangeParam == null) return true;

    final DateParam upperBound = dateRangeParam.getUpperBound();
    if (upperBound != null) {
      switch (upperBound.getPrefix()) {
        case LESSTHAN:
          if (upperBound.getValue().toInstant().isBefore(getFirstUpdated())) {
            return false;
          }
          break;
        case LESSTHAN_OR_EQUALS:
          if (!upperBound.getValue().toInstant().isAfter(getFirstUpdated())) {
            return false;
          }
          break;
        default:
          throw new InvalidRequestException("Invalid upper bound in _lastUpdated");
      }
    }

    final DateParam lowerBound = dateRangeParam.getLowerBound();
    if (lowerBound != null) {
      switch (lowerBound.getPrefix()) {
        case GREATERTHAN:
          if (!lowerBound.getValue().toInstant().isBefore(getLastUpdated())) {
            return false;
          }
          break;
        case GREATERTHAN_OR_EQUALS:
          if (lowerBound.getValue().toInstant().isAfter(getLastUpdated())) {
            return false;
          }
          break;
        default:
          throw new InvalidRequestException("Invalid lower bound in _lastUpdated");
      }
    }

    return true;
  }

  /**
   * Determines if the filter might contain the passed in beneficiary.
   *
   * @param beneficiaryId to test
   * @return true if the filter may contain the beneficiary
   */
  public boolean mightContain(Long beneficiaryId) {
    return updatedBeneficiaries.mightContain(beneficiaryId);
  }

  /**
   * Create a bloom filter with passed size.
   *
   * @param count to allocate
   * @return a new BloomFilter
   */
  public static BloomFilter createFilter(int count) {
    return BloomFilter.create(count, FALSE_POSITIVE_PERCENTAGE);
  }
}
