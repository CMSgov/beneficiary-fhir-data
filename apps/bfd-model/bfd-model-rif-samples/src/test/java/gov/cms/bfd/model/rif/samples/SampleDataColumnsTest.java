package gov.cms.bfd.model.rif.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.BeneficiaryColumn;
import gov.cms.bfd.model.rif.CarrierClaimColumn;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.parse.RifParsingUtils;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that the columns in the sample data match the columns in the various RIF column enums in
 * our Java ETL code, e.g. {@link BeneficiaryColumn}, {@link CarrierClaimColumn}.
 */
public final class SampleDataColumnsTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataColumnsTest.class);

  /** No assertions here: it just logs out the enum columns for posterity and other uses. */
  @Test
  public void logEnumColumns() {
    for (RifFileType rifFileType : RifFileType.values()) {
      Enum<?>[] columnsInEnum = getColumnsInEnum(rifFileType);
      LOGGER.info(
          "Enum columns for '{}': {}",
          columnsInEnum[0].getDeclaringClass().getName(),
          toHeaderFormat(columnsInEnum, c -> c.name()));
    }
  }

  /**
   * Checks our code's columns against the {@link
   * gov.cms.bfd.model.rif.samples.StaticRifResourceGroup#SAMPLE_A} data file headers.
   */
  @Test
  public void verifySampleAColumns() {
    verifyColumns(StaticRifResourceGroup.SAMPLE_A);
  }

  /**
   * Checks our code's columns against the {@link
   * gov.cms.bfd.model.rif.samples.StaticRifResourceGroup#SAMPLE_B} data file headers.
   */
  @Test
  // FIXME Temporary workaround until CBBD-253 and CBBD-283 are resolved.
  @Disabled
  public void verifySampleBColumns() {
    verifyColumns(StaticRifResourceGroup.SAMPLE_B);
  }

  /**
   * Checks our code's columns against the {@link
   * gov.cms.bfd.model.rif.samples.StaticRifResourceGroup#SAMPLE_MCT} data file headers.
   */
  @Test
  public void verifySampleMCTColumns() {
    verifyColumns(StaticRifResourceGroup.SAMPLE_MCT);
    verifyColumns(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_1);
    verifyColumns(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_2);
    verifyColumns(StaticRifResourceGroup.SAMPLE_MCT_UPDATE_3);
  }

  /**
   * Verifies that our code's column {@link Enum}s match the headers in the specified sample files.
   *
   * @param sampleGroup the sample data files to check the headers of
   */
  private void verifyColumns(StaticRifResourceGroup sampleGroup) {
    try {
      for (StaticRifResource sampleFile : sampleGroup.getResources()) {
        Enum<?>[] columnsInEnum = getColumnsInEnum(sampleFile.getRifFileType());

        // Use a CSVParser to parse the header out of the sample file.
        CSVFormat parserFormat = CSVFormat.DEFAULT.withDelimiter('|');
        CSVParser parser = RifParsingUtils.createCsvParser(parserFormat, sampleFile.toRifFile());
        CSVRecord sampleHeaderRecord = parser.getRecords().get(0);
        String[] columnsInSample = new String[sampleHeaderRecord.size()];
        for (int col = 0; col < columnsInSample.length; col++)
          columnsInSample[col] = sampleHeaderRecord.get(col);

        /*
         * Remove from consideration the processing metadata columns
         * that intentionally aren't in the enums.
         */
        List<String> metadataColumns = Arrays.asList("VERSION", "DML_IND");
        columnsInSample =
            Arrays.stream(columnsInSample)
                .filter(c -> !metadataColumns.contains(c))
                .toArray(String[]::new);

        assertEquals(
            columnsInSample.length,
            columnsInEnum.length,
            String.format(
                "Column count mismatch for '%s'.\nSample Columns: %s\nEnum Columns:   %s\n",
                sampleFile.name(),
                toHeaderFormat(columnsInSample, c -> c),
                toHeaderFormat(columnsInEnum, c -> c.name())));

        /*
         * Loop through the columns in the sample data and ensure that
         * our column enums match.
         */
        for (int col = 0; col < columnsInSample.length; col++) {
          String columnNameFromEnum = columnsInEnum[col].name();
          String columnNameFromSample = columnsInSample[col];
          assertEquals(
              columnNameFromSample,
              columnNameFromEnum,
              String.format(
                  "Unable to match column '%d' from sample data for '%s'.\nSample Columns: %s\nEnum Columns:   %s\n",
                  col,
                  sampleFile.name(),
                  toHeaderFormat(columnsInSample, c -> c),
                  toHeaderFormat(columnsInEnum, c -> c.name())));
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @param rifFileType the {@link RifFileType} to get the column {@link Enum}s for
   * @return the RIF column {@link Enum}s for the specified {@link RifFileType}
   */
  private static Enum<?>[] getColumnsInEnum(RifFileType rifFileType) {
    try {
      Enum<?>[] columnEnums =
          (Enum<?>[]) rifFileType.getColumnEnum().getMethod("values").invoke(null);
      return columnEnums;
    } catch (IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * @param columns the array of columns to format
   * @param columnNameFunction the {@link Function} to convert each column entry to the column name
   * @return the specified columns, but in the format used in RIF header rows
   */
  private static <T> String toHeaderFormat(T[] columns, Function<T, String> columnNameFunction) {
    StringBuilder formattedColumns = new StringBuilder();
    for (int i = 0; i < columns.length; i++) {
      formattedColumns.append(columnNameFunction.apply(columns[i]));
      if (i < (columns.length - 1)) formattedColumns.append('|');
    }
    return formattedColumns.toString();
  }
}
