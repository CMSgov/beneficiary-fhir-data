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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A one-off utility application for fixing the sample data that was produced on 2017-11-27. */
public final class SyntheticDataFixer {
  private static final Logger LOGGER = LoggerFactory.getLogger(SyntheticDataFixer.class);

  private static final Path PATH_ORIGINAL_DATA =
      Paths.get("/home/karl/workspaces/cms/bluebutton-synthetic-data-2017-11-27");
  private static final Path PATH_FIXED_DATA =
      Paths.get("/home/karl/workspaces/cms/bluebutton-synthetic-data-2017-11-27/fixed");

  private static final DateTimeFormatter RIF_DATE_FORMATTER_ORIGINAL =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("ddMMMyyyy")
          .toFormatter();
  private static final DateTimeFormatter RIF_DATE_FORMATTER_FIXED =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("dd-MMM-yyyy")
          .toFormatter();

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
    List<String> shuffledHicnsList = new LinkedList<>();
    for (int i = 0; i < 100000; i++) shuffledHicnsList.add("" + (1000000000 + i));
    Collections.shuffle(shuffledHicnsList);
    Deque<String> shuffledHicnsDeque = new LinkedBlockingDeque<>(shuffledHicnsList);
    Arrays.stream(SyntheticDataFile.values())
        .filter(r -> RifFileType.BENEFICIARY == r.getRifFile().getFileType())
        .forEach(r -> fixBeneficiaryFile(shuffledHicnsDeque, r));

    // Fix the Carrier files.
    Set<String> carrierClaimLineIds = new HashSet<>();
    Arrays.stream(SyntheticDataFile.values())
        .filter(r -> RifFileType.CARRIER == r.getRifFile().getFileType())
        .forEach(r -> fixCarrierFile(r, carrierClaimLineIds));

    // Fix the Inpatient files.
    Set<String> inpatientClaimLineIds = new HashSet<>();
    Arrays.stream(SyntheticDataFile.values())
        .filter(r -> RifFileType.INPATIENT == r.getRifFile().getFileType())
        .forEach(r -> fixInpatientFile(r, inpatientClaimLineIds));

    // Fix the Part D files.
    Arrays.stream(SyntheticDataFile.values())
        .filter(r -> RifFileType.PDE == r.getRifFile().getFileType())
        .forEach(r -> fixPartDEventsFile(r));
  }

  /**
   * Process the original RIF file for the specified {@link SyntheticDataFile} , then write out a
   * fixed version of the file.
   *
   * @param hicnPool the {@link Deque} of HICNs to select from, which will be depleted as rows are
   *     written
   * @param syntheticDataFile the beneficiary {@link SyntheticDataFile} to be fixed
   * @throws IOException (Any {@link IOException}s encountered will be bubbled up.
   */
  private static void fixBeneficiaryFile(
      Deque<String> hicnPool, SyntheticDataFile syntheticDataFile) {
    LocalRifFile rifFile = syntheticDataFile.getRifFile();
    CSVParser parser = RifParsingUtils.createCsvParser(rifFile);
    LOGGER.info("Fixing RIF file: '{}'...", rifFile.getDisplayName());

    /*
     * We tell the CSVPrinter not to include a header here, because we will
     * manually add it later, based on what we find in the input file.
     */
    CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader((String[]) null);
    try (FileWriter writer = new FileWriter(syntheticDataFile.getFixedFilePath().toFile());
        CSVPrinter rifFilePrinter = new CSVPrinter(writer, csvFormat); ) {

      /*
       * When we created the CSVPrinter, we told it to skip the header.
       * That ensures that we don't write out a header until we've started
       * reading the file and know what it is. Before proceeding, we
       * verify that the header is what we expect it to be, to avoid
       * propagating errors in our code.
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

            // Fix the not-random-enough HICNs.
            recordValues.set(BeneficiaryColumn.BENE_CRNT_HIC_NUM.ordinal() + 1, hicnPool.pop());

            // Fix the incorrectly formatted dates.
            fixDateFormatting(recordValues, BeneficiaryColumn.BENE_BIRTH_DT);

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
   * Process the original RIF file for the specified {@link SyntheticDataFile} , then write out a
   * fixed version of the file.
   *
   * @param syntheticDataFile the beneficiary {@link SyntheticDataFile} to be fixed
   * @param carrierClaimLineIds the {@link Set} of already-encountered <code>CLM_ID:LINE_NUM</code>
   *     pairs, which will be used to skip dupes
   * @throws IOException (Any {@link IOException}s encountered will be bubbled up.
   */
  private static void fixCarrierFile(
      SyntheticDataFile syntheticDataFile, Set<String> carrierClaimLineIds) {
    LocalRifFile rifFile = syntheticDataFile.getRifFile();
    CSVParser parser = RifParsingUtils.createCsvParser(rifFile);
    LOGGER.info("Fixing RIF file: '{}'...", rifFile.getDisplayName());

    /*
     * We tell the CSVPrinter not to include a header here, because we will
     * manually add it later, based on what we find in the input file.
     */
    CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader((String[]) null);
    try (FileWriter writer = new FileWriter(syntheticDataFile.getFixedFilePath().toFile());
        CSVPrinter rifFilePrinter = new CSVPrinter(writer, csvFormat); ) {

      /*
       * When we created the CSVPrinter, we told it to skip the header.
       * That ensures that we don't write out a header until we've started
       * reading the file and know what it is. Before proceeding, we
       * verify that the header is what we expect it to be, to avoid
       * propagating errors in our code.
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

            // Skip dupe PKs.
            String carrierClaimLineId =
                String.format(
                    "%s:%s",
                    recordValues.get(CarrierClaimColumn.CLM_ID.ordinal() + 1),
                    recordValues.get(CarrierClaimColumn.LINE_NUM.ordinal() + 1));
            if (carrierClaimLineIds.contains(carrierClaimLineId)) return;
            carrierClaimLineIds.add(carrierClaimLineId);

            // Fix the incorrectly formatted dates.
            fixDateFormatting(recordValues, CarrierClaimColumn.CLM_FROM_DT);
            fixDateFormatting(recordValues, CarrierClaimColumn.CLM_THRU_DT);
            fixDateFormatting(recordValues, CarrierClaimColumn.NCH_WKLY_PROC_DT);
            fixDateFormatting(recordValues, CarrierClaimColumn.LINE_1ST_EXPNS_DT);
            fixDateFormatting(recordValues, CarrierClaimColumn.LINE_LAST_EXPNS_DT);

            // Fix the incorrectly formatted numbers.
            fixNumberFormatting(recordValues, CarrierClaimColumn.CLM_PMT_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.CARR_CLM_PRMRY_PYR_PD_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.NCH_CLM_PRVDR_PMT_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.NCH_CLM_BENE_PMT_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.NCH_CARR_CLM_SBMTD_CHRG_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.NCH_CARR_CLM_ALOWD_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.CARR_CLM_CASH_DDCTBL_APLD_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_SRVC_CNT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_NCH_PMT_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_BENE_PMT_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_PRVDR_PMT_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_BENE_PTB_DDCTBL_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_BENE_PRMRY_PYR_PD_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_COINSRNC_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_SBMTD_CHRG_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_ALOWD_CHRG_AMT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.CARR_LINE_MTUS_CNT);
            fixNumberFormatting(recordValues, CarrierClaimColumn.LINE_HCT_HGB_RSLT_NUM);
            fixNumberFormatting(recordValues, CarrierClaimColumn.CARR_LINE_ANSTHSA_UNIT_CNT);

            // Fix the unexpected null values.
            replaceNullValue(recordValues, CarrierClaimColumn.CARR_LINE_PRVDR_TYPE_CD, "0");

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
   * Process the original RIF file for the specified {@link SyntheticDataFile} , then write out a
   * fixed version of the file.
   *
   * @param syntheticDataFile the beneficiary {@link SyntheticDataFile} to be fixed
   * @param inpatientClaimLineIds the {@link Set} of already-encountered <code>CLM_ID:REV_CNTR
   *     </code> pairs, which will be used to skip dupes
   * @throws IOException (Any {@link IOException}s encountered will be bubbled up.
   */
  private static void fixInpatientFile(
      SyntheticDataFile syntheticDataFile, Set<String> inpatientClaimLineIds) {
    LocalRifFile rifFile = syntheticDataFile.getRifFile();
    CSVParser parser = RifParsingUtils.createCsvParser(rifFile);
    LOGGER.info("Fixing RIF file: '{}'...", rifFile.getDisplayName());

    /*
     * We tell the CSVPrinter not to include a header here, because we will
     * manually add it later, based on what we find in the input file.
     */
    CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader((String[]) null);
    try (FileWriter writer = new FileWriter(syntheticDataFile.getFixedFilePath().toFile());
        CSVPrinter rifFilePrinter = new CSVPrinter(writer, csvFormat); ) {

      /*
       * When we created the CSVPrinter, we told it to skip the header.
       * That ensures that we don't write out a header until we've started
       * reading the file and know what it is. Before proceeding, we
       * verify that the header is what we expect it to be, to avoid
       * propagating errors in our code.
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

            // Skip dupe PKs.
            String inpatientClaimLineId =
                String.format(
                    "%s:%s",
                    recordValues.get(InpatientClaimColumn.CLM_ID.ordinal() + 1),
                    recordValues.get(InpatientClaimColumn.REV_CNTR.ordinal() + 1));
            if (inpatientClaimLineIds.contains(inpatientClaimLineId)) return;
            inpatientClaimLineIds.add(inpatientClaimLineId);

            // Fix the incorrectly formatted dates.
            fixDateFormatting(recordValues, InpatientClaimColumn.CLM_FROM_DT);
            fixDateFormatting(recordValues, InpatientClaimColumn.CLM_THRU_DT);
            fixDateFormatting(recordValues, InpatientClaimColumn.NCH_WKLY_PROC_DT);
            fixDateFormatting(recordValues, InpatientClaimColumn.FI_CLM_PROC_DT);
            fixDateFormatting(recordValues, InpatientClaimColumn.CLM_ADMSN_DT);
            fixDateFormatting(recordValues, InpatientClaimColumn.NCH_VRFD_NCVRD_STAY_FROM_DT);
            fixDateFormatting(recordValues, InpatientClaimColumn.NCH_VRFD_NCVRD_STAY_THRU_DT);
            // fixDateFormatting(recordValues,
            // InpatientClaimColumn.NCH_VRFD_NCVRD_STAY_THRU_DT);
            fixDateFormatting(recordValues, InpatientClaimColumn.NCH_ACTV_OR_CVRD_LVL_CARE_THRU);
            fixDateFormatting(recordValues, InpatientClaimColumn.NCH_BENE_DSCHRG_DT);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT1);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT2);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT3);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT4);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT5);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT6);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT7);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT8);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT9);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT10);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT11);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT12);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT13);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT14);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT15);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT16);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT17);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT18);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT19);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT20);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT21);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT22);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT23);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT24);
            fixDateFormatting(recordValues, InpatientClaimColumn.PRCDR_DT25);

            // Fix the incorrectly formatted numbers.
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PMT_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_PRMRY_PYR_CLM_PD_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_TOT_CHRG_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PASS_THRU_PER_DIEM_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_BENE_IP_DDCTBL_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_BENE_PTA_COINSRNC_LBLTY_AM);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_PROFNL_CMPNT_CHRG_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_IP_NCVRD_CHRG_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_IP_TOT_DDCTN_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_TOT_PPS_CPTL_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PPS_CPTL_FSP_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PPS_CPTL_OUTLIER_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PPS_CPTL_IME_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PPS_CPTL_EXCPTN_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_PPS_CPTL_DRG_WT_NUM);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_UTLZTN_DAY_CNT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.BENE_TOT_COINSRNC_DAYS_CNT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.BENE_LRD_USED_CNT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.CLM_NON_UTLZTN_DAYS_CNT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_BLOOD_PNTS_FRNSHD_QTY);
            fixNumberFormatting(recordValues, InpatientClaimColumn.NCH_DRG_OUTLIER_APRVD_PMT_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.IME_OP_CLM_VAL_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.DSH_OP_CLM_VAL_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.REV_CNTR_UNIT_CNT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.REV_CNTR_RATE_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.REV_CNTR_TOT_CHRG_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.REV_CNTR_NCVRD_CHRG_AMT);
            fixNumberFormatting(recordValues, InpatientClaimColumn.REV_CNTR_NDC_QTY);

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
   * Process the original RIF file for the specified {@link SyntheticDataFile} , then write out a
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
     * We tell the CSVPrinter not to include a header here, because we will
     * manually add it later, based on what we find in the input file.
     */
    CSVFormat csvFormat = RifParsingUtils.CSV_FORMAT.withHeader((String[]) null);
    try (FileWriter writer = new FileWriter(syntheticDataFile.getFixedFilePath().toFile());
        CSVPrinter rifFilePrinter = new CSVPrinter(writer, csvFormat); ) {

      /*
       * When we created the CSVPrinter, we told it to skip the header.
       * That ensures that we don't write out a header until we've started
       * reading the file and know what it is. Before proceeding, we
       * verify that the header is what we expect it to be, to avoid
       * propagating errors in our code.
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

            // Fix the incorrect null values.
            fixNullFormatting(recordValues, PartDEventColumn.CTSTRPHC_CVRG_CD);
            fixNullFormatting(recordValues, PartDEventColumn.BRND_GNRC_CD);
            fixNullFormatting(recordValues, PartDEventColumn.RX_ORGN_CD);

            // Fix the incorrectly formatted dates.
            fixDateFormatting(recordValues, PartDEventColumn.SRVC_DT);
            fixDateFormatting(recordValues, PartDEventColumn.PD_DT);

            // Fix the incorrectly formatted numbers.
            fixNumberFormatting(recordValues, PartDEventColumn.QTY_DSPNSD_NUM);
            fixNumberFormatting(recordValues, PartDEventColumn.DAYS_SUPLY_NUM);
            fixNumberFormatting(recordValues, PartDEventColumn.FILL_NUM);
            fixNumberFormatting(recordValues, PartDEventColumn.GDC_BLW_OOPT_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.GDC_ABV_OOPT_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.PTNT_PAY_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.OTHR_TROOP_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.LICS_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.PLRO_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.CVRD_D_PLAN_PD_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.NCVRD_PLAN_PD_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.TOT_RX_CST_AMT);
            fixNumberFormatting(recordValues, PartDEventColumn.RPTD_GAP_DSCNT_NUM);

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
   * Fixes the formatting of any null values in the specified column.
   *
   * @param recordValues the record with the column to be fixed
   * @param columnEnumConstant the RIF {@link Enum} constant for the column (e.g. {@link
   *     BeneficiaryColumn}) to be fixed
   */
  private static void fixNullFormatting(List<String> recordValues, Enum<?> columnEnumConstant) {
    if (recordValues.get(columnEnumConstant.ordinal() + 1).trim().equals("^"))
      recordValues.set(columnEnumConstant.ordinal() + 1, "");
  }

  /**
   * Fixes the formatting of the date in the specified column.
   *
   * @param recordValues the record with the column to be fixed
   * @param columnEnumConstant the RIF {@link Enum} constant for the column (e.g. {@link
   *     BeneficiaryColumn}) to be fixed
   */
  private static void fixDateFormatting(List<String> recordValues, Enum<?> columnEnumConstant) {
    String dateOriginalText = recordValues.get(columnEnumConstant.ordinal() + 1);
    if (dateOriginalText.trim().isEmpty()) return;

    TemporalAccessor dateOriginal = RIF_DATE_FORMATTER_ORIGINAL.parse(dateOriginalText);

    String dateFixedText = RIF_DATE_FORMATTER_FIXED.format(dateOriginal).toUpperCase();
    recordValues.set(columnEnumConstant.ordinal() + 1, dateFixedText);
  }

  /**
   * Fixes the formatting of the number in the specified column.
   *
   * @param recordValues the record with the column to be fixed
   * @param columnEnumConstant the RIF {@link Enum} constant for the column (e.g. {@link
   *     BeneficiaryColumn}) to be fixed
   */
  private static void fixNumberFormatting(List<String> recordValues, Enum<?> columnEnumConstant) {
    String numberOriginalText = recordValues.get(columnEnumConstant.ordinal() + 1);
    if (numberOriginalText.trim().isEmpty()) return;

    String numberFixedText = numberOriginalText.replaceAll(",", "");
    recordValues.set(columnEnumConstant.ordinal() + 1, numberFixedText);
  }

  /**
   * Replaces null values in the specified column.
   *
   * @param recordValues the record with the column to be fixed
   * @param columnEnumConstant the RIF {@link Enum} constant for the column (e.g. {@link
   *     BeneficiaryColumn}) to be fixed
   * @param replacementValue the value to replace null ones with
   */
  private static void replaceNullValue(
      List<String> recordValues, Enum<?> columnEnumConstant, String replacementValue) {
    String originalValue = recordValues.get(columnEnumConstant.ordinal() + 1);
    if (originalValue.isEmpty())
      recordValues.set(columnEnumConstant.ordinal() + 1, replacementValue);
  }

  /** Enumerates the synthetic data files to be fixed. */
  static enum SyntheticDataFile {
    BENEFICIARY_1999(
        RifFileType.BENEFICIARY, "Bene10k_production_s1999.txt", "synthetic-beneficiary-1999.rif"),

    BENEFICIARY_2000(
        RifFileType.BENEFICIARY, "Bene10k_production_s2000.txt", "synthetic-beneficiary-2000.rif"),

    BENEFICIARY_2014(
        RifFileType.BENEFICIARY, "Bene10k_production_s2014.txt", "synthetic-beneficiary-2014.rif"),

    CARRIER_1999_1999(
        RifFileType.CARRIER, "CARRCLM1999_ENCYP_S1999.txt", "synthetic-carrier-1999-1999.rif"),

    CARRIER_1999_2000(
        RifFileType.CARRIER, "CARRCLM2000_ENCYP_S1999.txt", "synthetic-carrier-1999-2000.rif"),

    CARRIER_1999_2001(
        RifFileType.CARRIER, "CARRCLM2001_ENCYP_S1999.txt", "synthetic-carrier-1999-2001.rif"),

    CARRIER_2000_2000(
        RifFileType.CARRIER, "CARRCLM2000_ENCYP_S2000.txt", "synthetic-carrier-2000-2000.rif"),

    CARRIER_2000_2001(
        RifFileType.CARRIER, "CARRCLM2001_ENCYP_S2000.txt", "synthetic-carrier-2000-2001.rif"),

    CARRIER_2000_2002(
        RifFileType.CARRIER, "CARRCLM2002_ENCYP_S2000.txt", "synthetic-carrier-2000-2002.rif"),

    CARRIER_2014_2014(
        RifFileType.CARRIER, "CARRCLM2014_ENCYP_S2014.txt", "synthetic-carrier-2014-2014.rif"),

    CARRIER_2014_2015(
        RifFileType.CARRIER, "CARRCLM2015_ENCYP_S2014.txt", "synthetic-carrier-2014-2015.rif"),

    CARRIER_2014_2016(
        RifFileType.CARRIER, "CARRCLM2016_ENCYP_S2014.txt", "synthetic-carrier-2014-2016.rif"),

    INPATIENT_1999_1999(
        RifFileType.INPATIENT, "INPCLM1999_ENCYP_S1999.txt", "synthetic-inpatient-1999-1999.rif"),

    INPATIENT_1999_2000(
        RifFileType.INPATIENT, "INPCLM2000_ENCYP_S1999.txt", "synthetic-inpatient-1999-2000.rif"),

    INPATIENT_1999_2001(
        RifFileType.INPATIENT, "INPCLM2001_ENCYP_S1999.txt", "synthetic-inpatient-1999-2001.rif"),

    INPATIENT_2000_2000(
        RifFileType.INPATIENT, "INPCLM2000_ENCYP_S2000.txt", "synthetic-inpatient-2000-2000.rif"),

    INPATIENT_2000_2001(
        RifFileType.INPATIENT, "INPCLM2001_ENCYP_S2000.txt", "synthetic-inpatient-2000-2001.rif"),

    INPATIENT_2000_2002(
        RifFileType.INPATIENT, "INPCLM2002_ENCYP_S2000.txt", "synthetic-inpatient-2000-2002.rif"),

    INPATIENT_2014_2014(
        RifFileType.INPATIENT, "INPCLM2014_ENCYP_S2014.txt", "synthetic-inpatient-2014-2014.rif"),

    INPATIENT_2014_2015(
        RifFileType.INPATIENT, "INPCLM2015_ENCYP_S2014.txt", "synthetic-inpatient-2014-2015.rif"),

    INPATIENT_2014_2016(
        RifFileType.INPATIENT, "INPCLM2016_ENCYP_S2014.txt", "synthetic-inpatient-2014-2016.rif"),

    PDE_2014(RifFileType.PDE, "bb_fhir_synpuf_final2014.txt", "synthetic-pde-2014.rif"),

    PDE_2015(RifFileType.PDE, "bb_fhir_synpuf_final2015.txt", "synthetic-pde-2015.rif"),

    PDE_2016(RifFileType.PDE, "bb_fhir_synpuf_final2016.txt", "synthetic-pde-2016.rif");

    private final RifFileType rifFileType;
    private final String fileNameOriginal;
    private final String fileNameFixed;

    /**
     * Enum constant constructor.
     *
     * @param rifFileType the value to use for {@link LocalRifFile#getFileType()}
     * @param fileNameOriginal the filename of the original {@link SyntheticDataFile} (under {@link
     *     SyntheticDataFixer#PATH_ORIGINAL_DATA})
     * @param fileNameOriginal the filename for the fixed {@link SyntheticDataFile} (under {@link
     *     SyntheticDataFixer#PATH_FIXED_DATA})
     */
    private SyntheticDataFile(
        RifFileType rifFileType, String fileNameOriginal, String fileNameFixed) {
      this.rifFileType = rifFileType;
      this.fileNameOriginal = fileNameOriginal;
      this.fileNameFixed = fileNameFixed;
    }

    /** @return a {@link LocalRifFile} representation of this {@link SyntheticDataFile} */
    public LocalRifFile getRifFile() {
      return new LocalRifFile(PATH_ORIGINAL_DATA.resolve(fileNameOriginal), rifFileType);
    }

    /** @return the {@link Path} that the original version of this file is at */
    public Path getOriginalFilePath() {
      return PATH_ORIGINAL_DATA.resolve(fileNameOriginal);
    }

    /** @return the {@link Path} that the fixed version of this file will be written to */
    public Path getFixedFilePath() {
      return PATH_FIXED_DATA.resolve(fileNameFixed);
    }
  }
}
