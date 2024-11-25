package gov.cms.bfd.pipeline.sharedutils.adapters;

import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Base class for the SAMHSA Adapters.
 *
 * @param <TClaim> Claim type
 * @param <TClaimLine> ClaimLine type.
 */
public abstract class SamhsaAdapterBase<TClaim, TClaimLine> {
  /** The Claim to process. */
  TClaim claim;

  /** The claimLines to process. */
  List<TClaimLine> claimLines;

  /** The claim's table. */
  String table;

  /** The claimLines' table. */
  String linesTable;

  /**
   * Retrieves a list of SAMHSA fields.
   *
   * @return {@link SamhsaFields}
   */
  public abstract List<SamhsaFields> getFields()
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

  /** The list of samhsa fields for this claims. */
  List<SamhsaFields> samhsaFields = new ArrayList<>();

  /**
   * Returns the SAMHSA methods for a given claim line.
   *
   * @param claimLine The claim line to use.
   * @return a map of samhsa methods along with the columns.
   */
  abstract Map<Supplier<Optional<String>>, String> getClaimLineMethods(TClaimLine claimLine);

  /**
   * Returns the SAMHSA methods for the current claim.
   *
   * @return a map of samhsa methods along with the columns.
   */
  abstract Map<Supplier<Optional<String>>, String> getClaimMethods();

  /**
   * Returns the line number for a claim line.
   *
   * @param line the claim line to sue.
   * @return the line number.
   */
  abstract Short getLineNum(TClaimLine line);

  /** The class for the claim. */
  Class claimClass;

  /** The class for the claimLines. */
  Class claimLineClass;

  /**
   * Constructor.
   *
   * @param claim The claim.
   * @param claimLines The claimLines.
   */
  public SamhsaAdapterBase(TClaim claim, List<TClaimLine> claimLines) {
    this.claimLines = claimLines;
    this.claim = claim;
    this.claimClass = claim.getClass();
    if (claimLines != null && !claimLines.isEmpty()) {
      claimLineClass = claimLines.getFirst().getClass();
    }
  }

  /** Gets the SAMHSA codes for a claim and its claim lines. */
  void getCodes() {
    iterateMethods(getClaimMethods(), table, null);
    for (TClaimLine line : claimLines) {
      iterateMethods(getClaimLineMethods(line), linesTable, getLineNum(line));
    }
  }

  /**
   * Iterates over a set of methods and sets a SamhsaFields objects with all of the SAMHSA codes.
   *
   * @param methods the methods to loop over.
   * @param table the table belonging to this set of methods.
   * @param lineNum the line number for the claim line. Will be null if this is not a claim line.
   */
  void iterateMethods(
      Map<Supplier<Optional<String>>, String> methods, String table, Short lineNum) {
    for (Map.Entry<Supplier<Optional<String>>, String> method : methods.entrySet()) {
      Optional<String> code = method.getKey().get();
      if (code.isPresent()) {
        SamhsaFields field =
            SamhsaFields.builder()
                .column(method.getValue())
                .code(code.get())
                .table(table)
                .lineNum(lineNum)
                .build();
        samhsaFields.add(field);
      }
    }
  }

  /**
   * Gets the from date of a claim.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   * @return a LocalDate object of the claim's from date.
   */
  public LocalDate getFromDate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    LocalDate date = (LocalDate) claimClass.getMethod("getDateFrom").invoke(claim);
    return date == null ? LocalDate.MAX : date;
  }

  /**
   * Gets the to date of a claim.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   * @return A LocalDate object of the claim's through date.
   */
  public LocalDate getThroughDate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    LocalDate date = (LocalDate) claimClass.getMethod("getDateThrough").invoke(claim);
    return date == null ? LocalDate.MAX : date;
  }
}
