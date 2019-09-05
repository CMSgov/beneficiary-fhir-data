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
import java.util.ArrayList;
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
 * A one-off utility application for modifying the fixed-with-negative-ids sample data that was
 * produced on 2017-11-27 and then fixed by {@link SyntheticDataFixer}. This class adds additional
 * beneficiary columns from the BSF file.
 */
public final class SyntheticDataFixer3 {
  private static final Logger LOGGER = LoggerFactory.getLogger(SyntheticDataFixer2.class);

  private static final Path PATH_ORIGINAL_DATA =
      Paths.get(
          "/Users/d6lu/workspaces/cms/bluebutton-data-synthetic/2017-11-27T00:00:00.000Z-fixed-with-negative-ids");
  private static final Path PATH_FIXED_DATA =
      Paths.get(
          "/Users/d6lu/workspaces/cms/bluebutton-data-synthetic/2017-11-27T00:00:00.000Z-fixed-with-negative-ids-and-enrollment-columns");

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

    // Add additional Beneficiary columns from BSF file (headings and sample data)
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

      Object[] columnNamesFromFile =
          parser.getHeaderMap().entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .map(e -> e.getKey())
              .toArray();

      // Adding additional Beneficiary column headings
      @SuppressWarnings({"unchecked", "rawtypes"})
      List<String> beneHeadings = new ArrayList(Arrays.asList(columnNamesFromFile));
      List<String> additionalBeneHeadings =
          Arrays.asList(
              "MBI_NUM",
              "DEATH_DT",
              "RFRNC_YR",
              "A_MO_CNT",
              "B_MO_CNT",
              "BUYIN_MO_CNT",
              "HMO_MO_CNT",
              "RDS_MO_CNT",
              "ENRL_SRC",
              "SAMPLE_GROUP",
              "EFIVEPCT",
              "CRNT_BIC",
              "AGE",
              "COVSTART",
              "DUAL_MO_CNT",
              "FIPS_STATE_CNTY_JAN_CD",
              "FIPS_STATE_CNTY_FEB_CD",
              "FIPS_STATE_CNTY_MAR_CD",
              "FIPS_STATE_CNTY_APR_CD",
              "FIPS_STATE_CNTY_MAY_CD",
              "FIPS_STATE_CNTY_JUN_CD",
              "FIPS_STATE_CNTY_JUL_CD",
              "FIPS_STATE_CNTY_AUG_CD",
              "FIPS_STATE_CNTY_SEPT_CD",
              "FIPS_STATE_CNTY_OCT_CD",
              "FIPS_STATE_CNTY_NOV_CD",
              "FIPS_STATE_CNTY_DEC_CD",
              "V_DOD_SW",
              "RTI_RACE_CD",
              "MDCR_STUS_JAN_CD",
              "MDCR_STUS_FEB_CD",
              "MDCR_STUS_MAR_CD",
              "MDCR_STUS_APR_CD",
              "MDCR_STUS_MAY_CD",
              "MDCR_STUS_JUN_CD",
              "MDCR_STUS_JUL_CD",
              "MDCR_STUS_AUG_CD",
              "MDCR_STUS_SEPT_CD",
              "MDCR_STUS_OCT_CD",
              "MDCR_STUS_NOV_CD",
              "MDCR_STUS_DEC_CD",
              "PLAN_CVRG_MO_CNT",
              "MDCR_ENTLMT_BUYIN_1_IND",
              "MDCR_ENTLMT_BUYIN_2_IND",
              "MDCR_ENTLMT_BUYIN_3_IND",
              "MDCR_ENTLMT_BUYIN_4_IND",
              "MDCR_ENTLMT_BUYIN_5_IND",
              "MDCR_ENTLMT_BUYIN_6_IND",
              "MDCR_ENTLMT_BUYIN_7_IND",
              "MDCR_ENTLMT_BUYIN_8_IND",
              "MDCR_ENTLMT_BUYIN_9_IND",
              "MDCR_ENTLMT_BUYIN_10_IND",
              "MDCR_ENTLMT_BUYIN_11_IND",
              "MDCR_ENTLMT_BUYIN_12_IND",
              "HMO_1_IND",
              "HMO_2_IND",
              "HMO_3_IND",
              "HMO_4_IND",
              "HMO_5_IND",
              "HMO_6_IND",
              "HMO_7_IND",
              "HMO_8_IND",
              "HMO_9_IND",
              "HMO_10_IND",
              "HMO_11_IND",
              "HMO_12_IND",
              "PTC_CNTRCT_JAN_ID",
              "PTC_CNTRCT_FEB_ID",
              "PTC_CNTRCT_MAR_ID",
              "PTC_CNTRCT_APR_ID",
              "PTC_CNTRCT_MAY_ID",
              "PTC_CNTRCT_JUN_ID",
              "PTC_CNTRCT_JUL_ID",
              "PTC_CNTRCT_AUG_ID",
              "PTC_CNTRCT_SEPT_ID",
              "PTC_CNTRCT_OCT_ID",
              "PTC_CNTRCT_NOV_ID",
              "PTC_CNTRCT_DEC_ID",
              "PTC_PBP_JAN_ID",
              "PTC_PBP_FEB_ID",
              "PTC_PBP_MAR_ID",
              "PTC_PBP_APR_ID",
              "PTC_PBP_MAY_ID",
              "PTC_PBP_JUN_ID",
              "PTC_PBP_JUL_ID",
              "PTC_PBP_AUG_ID",
              "PTC_PBP_SEPT_ID",
              "PTC_PBP_OCT_ID",
              "PTC_PBP_NOV_ID",
              "PTC_PBP_DEC_ID",
              "PTC_PLAN_TYPE_JAN_CD",
              "PTC_PLAN_TYPE_FEB_CD",
              "PTC_PLAN_TYPE_MAR_CD",
              "PTC_PLAN_TYPE_APR_CD",
              "PTC_PLAN_TYPE_MAY_CD",
              "PTC_PLAN_TYPE_JUN_CD",
              "PTC_PLAN_TYPE_JUL_CD",
              "PTC_PLAN_TYPE_AUG_CD",
              "PTC_PLAN_TYPE_SEPT_CD",
              "PTC_PLAN_TYPE_OCT_CD",
              "PTC_PLAN_TYPE_NOV_CD",
              "PTC_PLAN_TYPE_DEC_CD",
              "PTD_CNTRCT_JAN_ID",
              "PTD_CNTRCT_FEB_ID",
              "PTD_CNTRCT_MAR_ID",
              "PTD_CNTRCT_APR_ID",
              "PTD_CNTRCT_MAY_ID",
              "PTD_CNTRCT_JUN_ID",
              "PTD_CNTRCT_JUL_ID",
              "PTD_CNTRCT_AUG_ID",
              "PTD_CNTRCT_SEPT_ID",
              "PTD_CNTRCT_OCT_ID",
              "PTD_CNTRCT_NOV_ID",
              "PTD_CNTRCT_DEC_ID",
              "PTD_PBP_JAN_ID",
              "PTD_PBP_FEB_ID",
              "PTD_PBP_MAR_ID",
              "PTD_PBP_APR_ID",
              "PTD_PBP_MAY_ID",
              "PTD_PBP_JUN_ID",
              "PTD_PBP_JUL_ID",
              "PTD_PBP_AUG_ID",
              "PTD_PBP_SEPT_ID",
              "PTD_PBP_OCT_ID",
              "PTD_PBP_NOV_ID",
              "PTD_PBP_DEC_ID",
              "PTD_SGMT_JAN_ID",
              "PTD_SGMT_FEB_ID",
              "PTD_SGMT_MAR_ID",
              "PTD_SGMT_APR_ID",
              "PTD_SGMT_MAY_ID",
              "PTD_SGMT_JUN_ID",
              "PTD_SGMT_JUL_ID",
              "PTD_SGMT_AUG_ID",
              "PTD_SGMT_SEPT_ID",
              "PTD_SGMT_OCT_ID",
              "PTD_SGMT_NOV_ID",
              "PTD_SGMT_DEC_ID",
              "RDS_JAN_IND",
              "RDS_FEB_IND",
              "RDS_MAR_IND",
              "RDS_APR_IND",
              "RDS_MAY_IND",
              "RDS_JUN_IND",
              "RDS_JUL_IND",
              "RDS_AUG_IND",
              "RDS_SEPT_IND",
              "RDS_OCT_IND",
              "RDS_NOV_IND",
              "RDS_DEC_IND",
              "META_DUAL_ELGBL_STUS_JAN_CD",
              "META_DUAL_ELGBL_STUS_FEB_CD",
              "META_DUAL_ELGBL_STUS_MAR_CD",
              "META_DUAL_ELGBL_STUS_APR_CD",
              "META_DUAL_ELGBL_STUS_MAY_CD",
              "META_DUAL_ELGBL_STUS_JUN_CD",
              "META_DUAL_ELGBL_STUS_JUL_CD",
              "META_DUAL_ELGBL_STUS_AUG_CD",
              "META_DUAL_ELGBL_STUS_SEPT_CD",
              "META_DUAL_ELGBL_STUS_OCT_CD",
              "META_DUAL_ELGBL_STUS_NOV_CD",
              "META_DUAL_ELGBL_STUS_DEC_CD",
              "CST_SHR_GRP_JAN_CD",
              "CST_SHR_GRP_FEB_CD",
              "CST_SHR_GRP_MAR_CD",
              "CST_SHR_GRP_APR_CD",
              "CST_SHR_GRP_MAY_CD",
              "CST_SHR_GRP_JUN_CD",
              "CST_SHR_GRP_JUL_CD",
              "CST_SHR_GRP_AUG_CD",
              "CST_SHR_GRP_SEPT_CD",
              "CST_SHR_GRP_OCT_CD",
              "CST_SHR_GRP_NOV_CD",
              "CST_SHR_GRP_DEC_CD");
      beneHeadings.addAll(additionalBeneHeadings);

      /*
       * When we created the CSVPrinter, we told it to skip the header. That ensures
       * that we don't write out a header until we've started reading the file and
       * know what it is. Before proceeding, we verify that the header is what we
       * expect it to be, to avoid propagating errors in our code.
       */
      Object[] columnNamesFromFileWithoutMetadata =
          beneHeadings.stream().filter(c -> !c.equals("DML_IND")).toArray();
      Object[] columnNamesFromEnum =
          Arrays.stream(BeneficiaryColumn.values()).map(c -> c.name()).toArray();
      if (!Arrays.equals(columnNamesFromFileWithoutMetadata, columnNamesFromEnum))
        throw new IllegalStateException(
            String.format(
                "Column names mismatch:\nColumns from enum: %s\nColumns from file: %s",
                Arrays.toString(columnNamesFromEnum),
                Arrays.toString(columnNamesFromFileWithoutMetadata)));

      rifFilePrinter.printRecord(beneHeadings);
      parser.forEach(
          r -> {
            // Read the record into a List.
            List<String> recordValues = new LinkedList<>();
            for (String value : r) recordValues.add(value);
            try {
              // Adding additional Beneficiary column sample data
              String[] additionalBeneFields =
                  new String[] {
                    "3456789",
                    "17-Mar-1981",
                    "3",
                    "4",
                    "5",
                    "6",
                    "7",
                    "8",
                    "A",
                    "B",
                    "C",
                    "D",
                    "4",
                    "24-Mar-1983",
                    "5",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "4",
                    "4",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "5",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "D",
                    "D",
                    "D",
                    "D",
                    "D",
                    "D",
                    "D",
                    "D",
                    "D",
                    "D",
                    "D",
                    "D",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "C",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "AA",
                    "BB"
                  };
              recordValues.addAll(Arrays.asList(additionalBeneFields));

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

            // Nothing to do here; no fixes needed.

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

            // Nothing to do here; no fixes needed.

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

            // Nothing to do here; no fixes needed.

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
