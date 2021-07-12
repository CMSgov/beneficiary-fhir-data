package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.param.DateRangeParam;
import java.time.Instant;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/** As set of methods to help form JPA queries. */
public class QueryUtils {
  /**
   * Create a predicate for the lastUpdate field based on the passed _lastUpdated parameter range.
   *
   * @param cb to use
   * @param root to use
   * @param range to base the predicate on
   * @return a predicate on the lastUpdated field
   */
  public static Predicate createLastUpdatedPredicateInstant(
      CriteriaBuilder cb, Root<?> root, DateRangeParam range) {
    final Path<Instant> lastUpdatedPath = root.get("lastUpdated");
    final Instant lowerBound =
        range.getLowerBoundAsInstant() == null ? null : range.getLowerBoundAsInstant().toInstant();
    final Instant upperBound =
        range.getUpperBoundAsInstant() == null ? null : range.getUpperBoundAsInstant().toInstant();
    Predicate lowerBoundPredicate;
    Predicate upperBoundPredicate;

    if (lowerBound != null) {
      switch (range.getLowerBound().getPrefix()) {
        case GREATERTHAN_OR_EQUALS:
          lowerBoundPredicate = cb.greaterThanOrEqualTo(lastUpdatedPath, lowerBound);
          break;
        case GREATERTHAN:
          lowerBoundPredicate = cb.greaterThan(lastUpdatedPath, lowerBound);
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate lower bound has an invalid prefix");
      }
    } else {
      lowerBoundPredicate = null;
    }

    if (upperBound != null) {
      switch (range.getUpperBound().getPrefix()) {
        case LESSTHAN:
          upperBoundPredicate = cb.lessThan(lastUpdatedPath, upperBound);
          break;
        case LESSTHAN_OR_EQUALS:
          upperBoundPredicate = cb.lessThanOrEqualTo(lastUpdatedPath, upperBound);
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate upper bound has an invalid prefix");
      }
      if (lowerBoundPredicate == null) {
        return cb.or(cb.isNull(lastUpdatedPath), upperBoundPredicate);
      } else {
        return cb.and(lowerBoundPredicate, upperBoundPredicate);
      }
    } else {
      if (lowerBoundPredicate == null) {
        throw new IllegalArgumentException(
            ("_lastUpdate upper and lower bound cannot both be null"));
      } else {
        return lowerBoundPredicate;
      }
    }
  }

  /**
   * Create a predicate for the lastUpdate field based on the passed _lastUpdated parameter range.
   *
   * @param cb to use
   * @param root to use
   * @param range to base the predicate on
   * @return a predicate on the lastUpdated field
   */
  public static Predicate createLastUpdatedPredicate(
      CriteriaBuilder cb, Root<?> root, DateRangeParam range) {
    final Path<Instant> lastUpdatedPath = root.get("lastUpdated");
    final Instant lowerBound =
        range.getLowerBoundAsInstant() == null ? null : range.getLowerBoundAsInstant().toInstant();
    final Instant upperBound =
        range.getUpperBoundAsInstant() == null ? null : range.getUpperBoundAsInstant().toInstant();
    Predicate lowerBoundPredicate;
    Predicate upperBoundPredicate;

    if (lowerBound != null) {
      switch (range.getLowerBound().getPrefix()) {
        case GREATERTHAN_OR_EQUALS:
          lowerBoundPredicate = cb.greaterThanOrEqualTo(lastUpdatedPath, lowerBound);
          break;
        case GREATERTHAN:
          lowerBoundPredicate = cb.greaterThan(lastUpdatedPath, lowerBound);
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate lower bound has an invalid prefix");
      }
    } else {
      lowerBoundPredicate = null;
    }

    if (upperBound != null) {
      switch (range.getUpperBound().getPrefix()) {
        case LESSTHAN:
          upperBoundPredicate = cb.lessThan(lastUpdatedPath, upperBound);
          break;
        case LESSTHAN_OR_EQUALS:
          upperBoundPredicate = cb.lessThanOrEqualTo(lastUpdatedPath, upperBound);
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate upper bound has an invalid prefix");
      }
      if (lowerBoundPredicate == null) {
        return cb.or(cb.isNull(lastUpdatedPath), upperBoundPredicate);
      } else {
        return cb.and(lowerBoundPredicate, upperBoundPredicate);
      }
    } else {
      if (lowerBoundPredicate == null) {
        throw new IllegalArgumentException(
            ("_lastUpdate upper and lower bound cannot both be null"));
      } else {
        return lowerBoundPredicate;
      }
    }
  }

  /**
   * Create a predicate for the lastUpdate field based on the passed range.
   *
   * @param lastUpdated date to test. Maybe null.
   * @param range to base test against. Maybe null.
   * @return true iff within the range specified
   */
  public static boolean isInRange(Instant lastUpdated, DateRangeParam range) {
    if (range == null || range.isEmpty()) {
      return true;
    }
    // The null lastUpdated is considered to be a very early time for this calculation
    final long lastUpdatedMillis = lastUpdated == null ? 0L : lastUpdated.toEpochMilli();

    if (range.getLowerBound() != null) {
      final long lowerBoundMillis = range.getLowerBoundAsInstant().toInstant().toEpochMilli();
      switch (range.getLowerBound().getPrefix()) {
        case GREATERTHAN:
          if (lastUpdatedMillis <= lowerBoundMillis) {
            return false;
          }
          break;

        case GREATERTHAN_OR_EQUALS:
          if (lastUpdatedMillis < lowerBoundMillis) {
            return false;
          }
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate lower bound has an invalid prefix");
      }
    }

    if (range.getUpperBound() != null) {
      final long upperBoundMillis = range.getUpperBoundAsInstant().getTime();
      switch (range.getUpperBound().getPrefix()) {
        case LESSTHAN:
          if (lastUpdatedMillis >= upperBoundMillis) {
            return false;
          }
          break;
        case LESSTHAN_OR_EQUALS:
          if (lastUpdatedMillis > upperBoundMillis) {
            return false;
          }
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate upper bound has an invalid prefix");
      }
    }
    return true;
  }
}
