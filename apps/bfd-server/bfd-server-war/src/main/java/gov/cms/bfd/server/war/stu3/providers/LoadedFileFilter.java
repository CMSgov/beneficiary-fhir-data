package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import java.util.Date;
import org.apache.spark.util.sketch.BloomFilter;

/**
 * LoadedFile filters are used to determine if a given beneficiary was updated in particular loaded
 * file. Beneath the covers, they use BloomFilters (see <a
 * href="https://en.wikipedia.org/wiki/Bloom_filter">Bloom Filters</a>) which are space efficient.
 */
public class LoadedFileFilter {
  public static final double FALSE_POSITIVE_PERCENTAGE = 0.01;

  // The entry of the LoadedFiles table
  private final long loadedFileId;

  // batches in the filters
  private final int batchesCount;

  // The interval of time when the RIF load took place
  private final Date firstUpdated;
  private final Date lastUpdated;

  // The beneficiaries that were updated in the RIF load
  private final BloomFilter updatedBeneficiaries;

  /**
   * Build a filter for a LoadedFile
   *
   * @param loadedFileId for this filter
   * @param batchesCount of the number of batches in this filter
   * @param firstUpdated for this filter
   * @param lastUpdated for this filter
   * @param updatedBeneficiaries bloom filter for this filter
   */
  public LoadedFileFilter(
      long loadedFileId,
      int batchesCount,
      Date firstUpdated,
      Date lastUpdated,
      BloomFilter updatedBeneficiaries) {
    this.loadedFileId = loadedFileId;
    this.batchesCount = batchesCount;
    this.firstUpdated = firstUpdated;
    this.lastUpdated = lastUpdated;
    this.updatedBeneficiaries = updatedBeneficiaries;
  }

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
          if (upperBound.getValue().getTime() <= getFirstUpdated().getTime()) {
            return false;
          }
          break;
        case LESSTHAN_OR_EQUALS:
          if (upperBound.getValue().getTime() < getFirstUpdated().getTime()) {
            return false;
          }
          break;
        case EQUAL:
          if (upperBound.getValue().getTime() < getFirstUpdated().getTime()) {
            return false;
          }
          break;
        default:
          throw new IllegalArgumentException("Invalid upper bound in _lastUpdated");
      }
    }

    final DateParam lowerBound = dateRangeParam.getLowerBound();
    if (lowerBound != null) {
      switch (lowerBound.getPrefix()) {
        case GREATERTHAN:
          if (lowerBound.getValue().getTime() >= getLastUpdated().getTime()) {
            return false;
          }
          break;
        case GREATERTHAN_OR_EQUALS:
          if (lowerBound.getValue().getTime() > getLastUpdated().getTime()) {
            return false;
          }
          break;
        case EQUAL:
          if (lowerBound.getValue().getTime() > getLastUpdated().getTime()) {
            return false;
          }
          break;
        default:
          throw new IllegalArgumentException("Invalid lower bound in _lastUpdated");
      }
    }

    return true;
  }

  /**
   * Might the filter contain the passed in beneficiary
   *
   * @param beneficiaryId to test
   * @return true if the filter may contain the beneficiary
   */
  public boolean mightContain(String beneficiaryId) {
    return updatedBeneficiaries.mightContain(beneficiaryId);
  }

  /** @return the fileId */
  public long getLoadedFileId() {
    return loadedFileId;
  }

  /** @return the firstUpdated */
  public Date getFirstUpdated() {
    return firstUpdated;
  }

  /** @return the lastUpdated */
  public Date getLastUpdated() {
    return lastUpdated;
  }

  /** @return the updatedBeneficiaries */
  public BloomFilter getUpdatedBeneficiaries() {
    return updatedBeneficiaries;
  }

  /**
   * Create a bloom filter with passed size
   *
   * @param count to allocate
   * @return a new BloomFilter
   */
  public static BloomFilter createFilter(int count) {
    return BloomFilter.create(count, FALSE_POSITIVE_PERCENTAGE);
  }

  public int getBatchesCount() {
    return batchesCount;
  }
}
