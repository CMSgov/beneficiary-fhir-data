package gov.cms.bfd.pipeline.rif.extract.synthetic;

import gov.cms.bfd.model.rif.BeneficiaryColumn;
import gov.cms.bfd.model.rif.CarrierClaimColumn;
import gov.cms.bfd.model.rif.InpatientClaimColumn;
import gov.cms.bfd.model.rif.PartDEventColumn;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.parse.RifParsingUtils;
import gov.cms.bfd.pipeline.rif.extract.LocalRifFile;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A one-off utility application for modifying the previously-fixed the sample data that was
 * produced on 2017-11-27 and then fixed by {@link SyntheticDataFixer} (the first one, not this
 * second one).
 */
public final class SyntheticDataFixer2 {
  private static final Logger LOGGER = LoggerFactory.getLogger(SyntheticDataFixer2.class);

  private static final Path PATH_ORIGINAL_DATA =
      Paths.get("/home/karl/workspaces/cms/bluebutton-synthetic-data-2017-11-27/fixed");
  private static final Path PATH_FIXED_DATA =
      Paths.get(
          "/home/karl/workspaces/cms/bluebutton-synthetic-data-2017-11-27/fixed-with-negative-ids");

  /**
   * The application entry point/driver. Will read in the synthetic data files and then write out
   * fixed copies of them.
   *
   * @param args (not used)
   * @throws Exception Any {@link Exception}s encountered will be bubbled up, halting the
   *     application.
   */
  public static void main(String[] args) throws Exception {
    // Delete any existing fixed versions.
    if (Files.exists(PATH_FIXED_DATA))
      Files.walk(PATH_FIXED_DATA)
          .map(Path::toFile)
          .sorted((o1, o2) -> -o1.compareTo(o2))
          .forEach(File::delete);

    // Ensure the output directory exists.
    Files.createDirectories(PATH_FIXED_DATA);

    // Fix the beneficiary files first.
    Arrays.stream(SyntheticDataFile.values())
        .filter(r -> RifFileType.BENEFICIARY == r.getRifFile().getFileType())
        .forEach(r -> fixBeneficiaryFile(r));

    // Fix the Carrier files.
    Arrays.stream(SyntheticDataFile.values())
        .filter(r -> RifFileType.CARRIER == r.getRifFile().getFileType())
        .forEach(r -> fixCarrierFile(r));

    // Fix the Inpatient files.
    Arrays.stream(SyntheticDataFile.values())
        .filter(r -> RifFileType.INPATIENT == r.getRifFile().getFileType())
        .forEach(r -> fixInpatientFile(r));

    // Fix the Part D files.
    Arrays.stream(SyntheticDataFile.values())
        .filter(r -> RifFileType.PDE == r.getRifFile().getFileType())
        .forEach(r -> fixPartDEventsFile(r));
  }

  /**
   * Process the original RIF file for the specified {@link SyntheticDataFile}, then write out a
   * fixed version of the file.
   *
   * @param syntheticDataFile the beneficiary {@link SyntheticDataFile} to be fixed
   * @throws IOException (Any {@link IOException}s encountered will be bubbled up.
   */
  private static void fixBeneficiaryFile(SyntheticDataFile syntheticDataFile) {
    LocalRifFile rifFile = syntheticDataFile.getRifFile();
    CSVParser parser = RifParsingUtils.createCsvParser(rifFile);
    LOGGER.info("Fixing RIF file: '{}'...", rifFile.getDisplayName());

    /*
     * We tell the CSVPrinter not to include a header here, because we will manually
     * add it later, based on what we find in the input file.
     */
    CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader((String[]) null);
    try (FileWriter writer = new FileWriter(syntheticDataFile.getFixedFilePath().toFile());
        CSVPrinter rifFilePrinter = new CSVPrinter(writer, csvFormat); ) {

      /*
       * When we created the CSVPrinter, we told it to skip the header. That ensures
       * that we don't write out a header until we've started reading the file and
       * know what it is. Before proceeding, we verify that the header is what we
       * expect it to be, to avoid propagating errors in our code.
       */
      Object[] columnNamesFromFile =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .toArray();
      Object[] columnNamesFromFileWithoutMetadata =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .filter(c -> !c.equals("DML_IND"))
              .toArray();
      Object[] columnNamesFromEnum =
          Arrays.stream(BeneficiaryColumn.values()).map(c -> c.name()).toArray();
      if (!Arrays.equals(columnNamesFromFileWithoutMetadata, columnNamesFromEnum))
        throw new IllegalStateException(
            String.format(
                "Column names mismatch:\nColumns from enum: %s\nColumns from file: %s",
                Arrays.toString(columnNamesFromEnum),
                Arrays.toString(columnNamesFromFileWithoutMetadata)));
      rifFilePrinter.printRecord(columnNamesFromFile);

      parser.forEach(
          r -> {
            // Read the record into a List.
            List<String> recordValues = new LinkedList<>();
            for (String value : r) recordValues.add(value);

            // Make the BENE_ID negative.
            makeColumnNegative(recordValues, BeneficiaryColumn.BENE_ID);

            try {
              rifFilePrinter.printRecord(recordValues);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    LOGGER.info("Fixed RIF file: '{}'...", syntheticDataFile.getFixedFilePath());
  }

  /**
   * Process the original RIF file for the specified {@link SyntheticDataFile}, then write out a
   * fixed version of the file.
   *
   * @param syntheticDataFile the beneficiary {@link SyntheticDataFile} to be fixed
   * @throws IOException (Any {@link IOException}s encountered will be bubbled up.
   */
  private static void fixCarrierFile(SyntheticDataFile syntheticDataFile) {
    LocalRifFile rifFile = syntheticDataFile.getRifFile();
    CSVParser parser = RifParsingUtils.createCsvParser(rifFile);
    LOGGER.info("Fixing RIF file: '{}'...", rifFile.getDisplayName());

    /*
     * We tell the CSVPrinter not to include a header here, because we will manually
     * add it later, based on what we find in the input file.
     */
    CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader((String[]) null);
    try (FileWriter writer = new FileWriter(syntheticDataFile.getFixedFilePath().toFile());
        CSVPrinter rifFilePrinter = new CSVPrinter(writer, csvFormat); ) {

      /*
       * When we created the CSVPrinter, we told it to skip the header. That ensures
       * that we don't write out a header until we've started reading the file and
       * know what it is. Before proceeding, we verify that the header is what we
       * expect it to be, to avoid propagating errors in our code.
       */
      Object[] columnNamesFromFile =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .toArray();
      Object[] columnNamesFromFileWithoutMetadata =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .filter(c -> !c.equals("DML_IND"))
              .toArray();
      Object[] columnNamesFromEnum =
          Arrays.stream(CarrierClaimColumn.values()).map(c -> c.name()).toArray();
      if (!Arrays.equals(columnNamesFromFileWithoutMetadata, columnNamesFromEnum))
        throw new IllegalStateException(
            String.format(
                "Column names mismatch:\nColumns from enum: %s\nColumns from file: %s",
                Arrays.toString(columnNamesFromEnum),
                Arrays.toString(columnNamesFromFileWithoutMetadata)));
      rifFilePrinter.printRecord(columnNamesFromFile);

      parser.forEach(
          r -> {
            // Read the record into a List.
            List<String> recordValues = new LinkedList<>();
            for (String value : r) recordValues.add(value);

            // Make the CLM_ID negative.
            makeColumnNegative(recordValues, CarrierClaimColumn.CLM_ID);

            // Make the BENE_ID negative.
            makeColumnNegative(recordValues, CarrierClaimColumn.BENE_ID);

            try {
              rifFilePrinter.printRecord(recordValues);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    LOGGER.info("Fixed RIF file: '{}'...", syntheticDataFile.getFixedFilePath());
  }

  /**
   * Process the original RIF file for the specified {@link SyntheticDataFile}, then write out a
   * fixed version of the file.
   *
   * @param syntheticDataFile the beneficiary {@link SyntheticDataFile} to be fixed
   * @throws IOException (Any {@link IOException}s encountered will be bubbled up.
   */
  private static void fixInpatientFile(SyntheticDataFile syntheticDataFile) {
    LocalRifFile rifFile = syntheticDataFile.getRifFile();
    CSVParser parser = RifParsingUtils.createCsvParser(rifFile);
    LOGGER.info("Fixing RIF file: '{}'...", rifFile.getDisplayName());

    /*
     * We tell the CSVPrinter not to include a header here, because we will manually
     * add it later, based on what we find in the input file.
     */
    CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader((String[]) null);
    try (FileWriter writer = new FileWriter(syntheticDataFile.getFixedFilePath().toFile());
        CSVPrinter rifFilePrinter = new CSVPrinter(writer, csvFormat); ) {

      /*
       * When we created the CSVPrinter, we told it to skip the header. That ensures
       * that we don't write out a header until we've started reading the file and
       * know what it is. Before proceeding, we verify that the header is what we
       * expect it to be, to avoid propagating errors in our code.
       */
      Object[] columnNamesFromFile =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .toArray();
      Object[] columnNamesFromFileWithoutMetadata =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .filter(c -> !c.equals("DML_IND"))
              .toArray();
      Object[] columnNamesFromEnum =
          Arrays.stream(InpatientClaimColumn.values()).map(c -> c.name()).toArray();
      if (!Arrays.equals(columnNamesFromFileWithoutMetadata, columnNamesFromEnum))
        throw new IllegalStateException(
            String.format(
                "Column names mismatch:\nColumns from enum: %s\nColumns from file: %s",
                Arrays.toString(columnNamesFromEnum),
                Arrays.toString(columnNamesFromFileWithoutMetadata)));
      rifFilePrinter.printRecord(columnNamesFromFile);

      parser.forEach(
          r -> {
            // Read the record into a List.
            List<String> recordValues = new LinkedList<>();
            for (String value : r) recordValues.add(value);

            // Make the CLM_ID negative.
            makeColumnNegative(recordValues, InpatientClaimColumn.CLM_ID);

            // Make the BENE_ID negative.
            makeColumnNegative(recordValues, InpatientClaimColumn.BENE_ID);

            try {
              rifFilePrinter.printRecord(recordValues);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    LOGGER.info("Fixed RIF file: '{}'...", syntheticDataFile.getFixedFilePath());
  }

  /**
   * Process the original RIF file for the specified {@link SyntheticDataFile}, then write out a
   * fixed version of the file.
   *
   * @param syntheticDataFile the beneficiary {@link SyntheticDataFile} to be fixed
   * @throws IOException (Any {@link IOException}s encountered will be bubbled up.
   */
  private static void fixPartDEventsFile(SyntheticDataFile syntheticDataFile) {
    LocalRifFile rifFile = syntheticDataFile.getRifFile();
    CSVParser parser = RifParsingUtils.createCsvParser(rifFile);
    LOGGER.info("Fixing RIF file: '{}'...", rifFile.getDisplayName());

    /*
     * We tell the CSVPrinter not to include a header here, because we will manually
     * add it later, based on what we find in the input file.
     */
    CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader((String[]) null);
    try (FileWriter writer = new FileWriter(syntheticDataFile.getFixedFilePath().toFile());
        CSVPrinter rifFilePrinter = new CSVPrinter(writer, csvFormat); ) {

      /*
       * When we created the CSVPrinter, we told it to skip the header. That ensures
       * that we don't write out a header until we've started reading the file and
       * know what it is. Before proceeding, we verify that the header is what we
       * expect it to be, to avoid propagating errors in our code.
       */
      Object[] columnNamesFromFile =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .toArray();
      Object[] columnNamesFromFileWithoutMetadata =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .filter(c -> !c.equals("DML_IND"))
              .toArray();
      Object[] columnNamesFromEnum =
          Arrays.stream(PartDEventColumn.values()).map(c -> c.name()).toArray();
      if (!Arrays.equals(columnNamesFromFileWithoutMetadata, columnNamesFromEnum))
        throw new IllegalStateException(
            String.format(
                "Column names mismatch:\nColumns from enum: %s\nColumns from file: %s",
                Arrays.toString(columnNamesFromEnum),
                Arrays.toString(columnNamesFromFileWithoutMetadata)));
      rifFilePrinter.printRecord(columnNamesFromFile);

      parser.forEach(
          r -> {
            // Read the record into a List.
            List<String> recordValues = new LinkedList<>();
            for (String value : r) recordValues.add(value);

            // Make the PDE_ID negative.
            makeColumnNegative(recordValues, PartDEventColumn.PDE_ID);

            // Make the BENE_ID negative.
            makeColumnNegative(recordValues, PartDEventColumn.BENE_ID);

            try {
              rifFilePrinter.printRecord(recordValues);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    LOGGER.info("Fixed RIF file: '{}'...", syntheticDataFile.getFixedFilePath());
  }

  /**
   * Makes the values in the specified column "negative" by prefixing them with a '<code>-</code>'
   * character.
   *
   * @param recordValues the record with the column to be modified
   * @param columnEnumConstant the RIF {@link Enum} constant for the column (e.g. {@link
   *     BeneficiaryColumn}) to be fixed
   */
  private static void makeColumnNegative(List<String> recordValues, Enum<?> columnEnumConstant) {
    String originalValue = recordValues.get(columnEnumConstant.ordinal() + 1);
    String modifiedValue = "-" + originalValue.trim();

    recordValues.set(columnEnumConstant.ordinal() + 1, modifiedValue);
  }

  /** Enumerates the synthetic data files to be fixed. */
  static enum SyntheticDataFile {
    BENEFICIARY_1999(RifFileType.BENEFICIARY, "synthetic-beneficiary-1999.rif"),

    BENEFICIARY_2000(RifFileType.BENEFICIARY, "synthetic-beneficiary-2000.rif"),

    BENEFICIARY_2014(RifFileType.BENEFICIARY, "synthetic-beneficiary-2014.rif"),

    CARRIER_1999_1999(RifFileType.CARRIER, "synthetic-carrier-1999-1999.rif"),

    CARRIER_1999_2000(RifFileType.CARRIER, "synthetic-carrier-1999-2000.rif"),

    CARRIER_1999_2001(RifFileType.CARRIER, "synthetic-carrier-1999-2001.rif"),

    CARRIER_2000_2000(RifFileType.CARRIER, "synthetic-carrier-2000-2000.rif"),

    CARRIER_2000_2001(RifFileType.CARRIER, "synthetic-carrier-2000-2001.rif"),

    CARRIER_2000_2002(RifFileType.CARRIER, "synthetic-carrier-2000-2002.rif"),

    CARRIER_2014_2014(RifFileType.CARRIER, "synthetic-carrier-2014-2014.rif"),

    CARRIER_2014_2015(RifFileType.CARRIER, "synthetic-carrier-2014-2015.rif"),

    CARRIER_2014_2016(RifFileType.CARRIER, "synthetic-carrier-2014-2016.rif"),

    INPATIENT_1999_1999(RifFileType.INPATIENT, "synthetic-inpatient-1999-1999.rif"),

    INPATIENT_1999_2000(RifFileType.INPATIENT, "synthetic-inpatient-1999-2000.rif"),

    INPATIENT_1999_2001(RifFileType.INPATIENT, "synthetic-inpatient-1999-2001.rif"),

    INPATIENT_2000_2000(RifFileType.INPATIENT, "synthetic-inpatient-2000-2000.rif"),

    INPATIENT_2000_2001(RifFileType.INPATIENT, "synthetic-inpatient-2000-2001.rif"),

    INPATIENT_2000_2002(RifFileType.INPATIENT, "synthetic-inpatient-2000-2002.rif"),

    INPATIENT_2014_2014(RifFileType.INPATIENT, "synthetic-inpatient-2014-2014.rif"),

    INPATIENT_2014_2015(RifFileType.INPATIENT, "synthetic-inpatient-2014-2015.rif"),

    INPATIENT_2014_2016(RifFileType.INPATIENT, "synthetic-inpatient-2014-2016.rif"),

    PDE_2014(RifFileType.PDE, "synthetic-pde-2014.rif"),

    PDE_2015(RifFileType.PDE, "synthetic-pde-2015.rif"),

    PDE_2016(RifFileType.PDE, "synthetic-pde-2016.rif");

    private final RifFileType rifFileType;
    private final String fileName;

    /**
     * Enum constant constructor.
     *
     * @param rifFileType the value to use for {@link LocalRifFile#getFileType()}
     * @param fileName the filename for the {@link SyntheticDataFile} (under both {@link
     *     SyntheticDataFixer2#PATH_ORIGINAL_DATA} and {@link SyntheticDataFixer2#PATH_FIXED_DATA} )
     */
    private SyntheticDataFile(RifFileType rifFileType, String fileName) {
      this.rifFileType = rifFileType;
      this.fileName = fileName;
    }

    /** @return a {@link LocalRifFile} representation of this {@link SyntheticDataFile} */
    public LocalRifFile getRifFile() {
      return new LocalRifFile(PATH_ORIGINAL_DATA.resolve(fileName), rifFileType);
    }

    /** @return the {@link Path} that the original version of this file is at */
    public Path getOriginalFilePath() {
      return PATH_ORIGINAL_DATA.resolve(fileName);
    }

    /** @return the {@link Path} that the fixed version of this file will be written to */
    public Path getFixedFilePath() {
      return PATH_FIXED_DATA.resolve(fileName);
    }
  }
}
